package javax.edi.service.channel;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.entity.CommunicationChannel;
import javax.edi.service.entity.CommunicationChannel.ChannelStatus;
import javax.edi.service.entity.CommunicationChannel.Direction;
import javax.edi.service.entity.CommunicationChannel.Protocol;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.entity.TradingPartner;
import javax.edi.service.registry.EDIModelRegistry;
import javax.edi.service.repository.CommunicationChannelRepository;
import javax.edi.service.repository.TradingPartnerRepository;
import javax.edi.service.service.TransactionLoggingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Unified channel-based polling service.
 * Iterates all ACTIVE INBOUND channels (SFTP, FTP) on a schedule,
 * reads EDI files, parses, logs, generates 997, delivers 997 via
 * the partner's OUTBOUND channel.
 */
@Service
public class ChannelPollingService {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelPollingService.class);

    @Value("${edi.channel.polling-enabled:false}")
    private boolean pollingEnabled;

    @Autowired private CommunicationChannelRepository channelRepo;
    @Autowired private TradingPartnerRepository partnerRepo;
    @Autowired private EDIModelRegistry modelRegistry;
    @Autowired private TransactionLoggingService loggingService;
    @Autowired private SftpChannelAdapter sftpAdapter;
    @Autowired private FtpChannelAdapter ftpAdapter;
    @Autowired private Oftp2ChannelAdapter oftp2Adapter;

    /**
     * Scheduled poll of all active inbound SFTP/FTP channels.
     */
    @Scheduled(fixedDelayString = "${edi.channel.poll-interval-ms:300000}",
               initialDelayString = "${edi.channel.initial-delay-ms:30000}")
    public void pollAllChannels() {
        if (!pollingEnabled) return;

        List<CommunicationChannel> channels = channelRepo.findByStatusAndDirectionAndProtocolIn(
                ChannelStatus.ACTIVE, Direction.INBOUND,
                Arrays.asList(Protocol.SFTP, Protocol.FTP, Protocol.OFTP2));

        if (channels.isEmpty()) {
            LOG.debug("Channel poll: no active inbound SFTP/FTP channels");
            return;
        }

        LOG.info("Channel poll started: {} channel(s)", channels.size());

        for (CommunicationChannel channel : channels) {
            try {
                pollChannel(channel);
            } catch (Exception e) {
                LOG.error("Channel poll failed for {} (partner={}): {}",
                        channel.getChannelName(), channel.getPartnerId(), e.getMessage());
                channel.setLastErrorAt(LocalDateTime.now());
                channel.setLastErrorMessage(e.getMessage());
                channel.setUpdatedAt(LocalDateTime.now());
                channelRepo.save(channel);
            }
        }
    }

    /**
     * Poll a single inbound channel.
     */
    public void pollChannel(CommunicationChannel channel) {
        LOG.info("Polling channel: {} [{}] partner={}",
                channel.getChannelName(), channel.getProtocol(), channel.getPartnerId());

        ChannelAdapter adapter = resolveAdapter(channel);

        // Look up partner for delimiter config and auto-ack
        TradingPartner partner = partnerRepo.findByPartnerId(channel.getPartnerId()).orElse(null);

        List<ChannelAdapter.RemoteFile> files = adapter.listFiles(channel);
        channel.setLastPolledAt(LocalDateTime.now());
        int processed = 0;

        for (ChannelAdapter.RemoteFile file : files) {
            // Apply file pattern filter
            if (channel.getFilePattern() != null && !channel.getFilePattern().isEmpty()) {
                String regex = channel.getFilePattern().replace(".", "\\.").replace("*", ".*");
                if (!file.getName().matches(regex)) continue;
            }

            try {
                String ediContent = adapter.readFile(channel, file.getPath());
                if (ediContent == null || ediContent.trim().isEmpty()) continue;

                // Detect transaction type using partner's delimiter config
                EDIDelimiterDetector delimiters = EDIDelimiterDetector.detect(ediContent, partner);
                String txnType = delimiters.detectTransactionType(ediContent);

                // Resolve model
                Class<?> modelClass = modelRegistry.resolve(txnType);
                if (modelClass == null) {
                    loggingService.logFailure(ediContent, txnType,
                            "Unknown transaction type: " + txnType,
                            channel.getPartnerId(),
                            EDITransactionLog.Source.SFTP, file.getName());
                    continue;
                }

                // Parse with resolved delimiters (partner config â ISA header â defaults)
                EDIUnmarshalResult<?> parseResult = EDIUnmarshaller.unmarshalResult(
                        modelClass, new StringReader(ediContent), delimiters.toMessageConfiguration());

                // 997
                EDI997Generator.Result ackResult = null;
                if (partner != null && partner.isAutoAcknowledge()) {
                    ackResult = EDI997Generator.generate(parseResult);
                }

                // Log
                EDITransactionLog txnLog = loggingService.logInboundTransaction(
                        ediContent, txnType, parseResult, ackResult,
                        channel.getPartnerId(),
                        channel.getProtocol() == Protocol.SFTP ? EDITransactionLog.Source.SFTP : EDITransactionLog.Source.BATCH,
                        file.getName());

                // Deliver 997 via outbound channel of same partner
                if (ackResult != null && ackResult.isSuccess()) {
                    deliver997(channel.getPartnerId(), ackResult, txnLog.getTransactionId());
                }

                // Archive or delete
                if (channel.getArchiveDirectory() != null && !channel.getArchiveDirectory().isEmpty()) {
                    adapter.moveFile(channel, file.getPath(),
                            channel.getArchiveDirectory() + "/" + file.getName());
                } else {
                    adapter.deleteFile(channel, file.getPath());
                }

                processed++;

            } catch (Exception e) {
                LOG.error("Failed to process file {} on channel {}: {}",
                        file.getName(), channel.getChannelName(), e.getMessage());
                loggingService.logFailure(null, "UNKNOWN",
                        "Processing failed: " + e.getMessage(),
                        channel.getPartnerId(),
                        EDITransactionLog.Source.SFTP, file.getName());
            }
        }

        channel.setLastSuccessAt(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        channelRepo.save(channel);
        LOG.info("Channel poll complete: {} ÃÂ¢ÃÂÃÂ {} file(s) processed", channel.getChannelName(), processed);
    }

    /**
     * Deliver 997 via the partner's outbound channel (SFTP, FTP, or HTTP).
     */
    private void deliver997(String partnerId, EDI997Generator.Result ackResult, String transactionId) {
        List<CommunicationChannel> outbound = channelRepo.findByPartnerIdAndDirection(
                partnerId, Direction.OUTBOUND);

        for (CommunicationChannel outCh : outbound) {
            if (outCh.getStatus() != ChannelStatus.ACTIVE) continue;
            try {
                ChannelAdapter adapter = resolveAdapter(outCh);
                String fileName = "997_" + ackResult.getControlNumber() + ".edi";
                String dir = outCh.getRemoteDirectory() != null ? outCh.getRemoteDirectory() : "/";
                adapter.writeFile(outCh, dir + "/" + fileName, ackResult.getEdiOutput());
                loggingService.markAcknowledgementDelivered(transactionId);
                LOG.info("997 delivered via {} to partner {}", outCh.getProtocol(), partnerId);
                return; // Delivered via first available channel
            } catch (Exception e) {
                LOG.error("997 delivery failed via {} for partner {}: {}",
                        outCh.getProtocol(), partnerId, e.getMessage());
            }
        }
    }

    private ChannelAdapter resolveAdapter(CommunicationChannel channel) {
        switch (channel.getProtocol()) {
            case SFTP:  return sftpAdapter;
            case FTP:   return ftpAdapter;
            case OFTP2: return oftp2Adapter;
            default:
                throw new UnsupportedOperationException(
                        "Polling not supported for protocol: " + channel.getProtocol());
        }
    }

}
