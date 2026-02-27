package javax.edi.service.sftp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;
import javax.edi.service.channel.EDIDelimiterDetector;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.entity.TradingPartner;
import javax.edi.service.registry.EDIModelRegistry;
import javax.edi.service.service.TransactionLoggingService;
import javax.edi.service.service.TradingPartnerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;

/**
 * SFTP polling service that:
 * 1. Connects to each active trading partner's SFTP server
 * 2. Reads EDI files from the inbound directory
 * 3. Parses them, logs them, generates 997 acknowledgements
 * 4. Drops the 997 into the outbound directory
 * 5. Moves the original file to archive
 * 
 * Runs on a configurable schedule (default: every 5 minutes).
 * 
 * Enable with:
 *   edi.sftp.polling-enabled=true
 *   edi.sftp.poll-interval-ms=300000
 */
@Service
public class SftpPollingService {

    private static final Logger LOG = LoggerFactory.getLogger(SftpPollingService.class);

    @Value("${edi.sftp.polling-enabled:false}")
    private boolean pollingEnabled;

    @Value("${edi.sftp.connection-timeout:10000}")
    private int connectionTimeout;

    @Autowired
    private TradingPartnerService partnerService;

    @Autowired
    private EDIModelRegistry modelRegistry;

    @Autowired
    private TransactionLoggingService loggingService;

    /**
     * Scheduled polling task. Runs every edi.sftp.poll-interval-ms milliseconds.
     */
    @Scheduled(fixedDelayString = "${edi.sftp.poll-interval-ms:300000}",
               initialDelayString = "${edi.sftp.initial-delay-ms:30000}")
    public void pollAllPartners() {
        if (!pollingEnabled) return;

        List<TradingPartner> partners = partnerService.getPartnersWithSftp();
        if (partners.isEmpty()) {
            LOG.debug("SFTP poll: no active partners with SFTP configuration");
            return;
        }

        LOG.info("SFTP poll started: {} partner(s) to check", partners.size());

        for (TradingPartner partner : partners) {
            try {
                pollPartner(partner);
            } catch (Exception e) {
                LOG.error("SFTP poll failed for partner {}: {}", partner.getPartnerId(), e.getMessage(), e);
            }
        }

        LOG.info("SFTP poll completed for {} partner(s)", partners.size());
    }

    /**
     * Polls a single partner's SFTP server for new EDI files.
     */
    public void pollPartner(TradingPartner partner) {
        LOG.info("Polling SFTP for partner: {} ({}:{})",
                partner.getPartnerId(), partner.getSftpHost(), partner.getSftpPort());

        Session session = null;
        ChannelSftp sftp = null;

        try {
            // Connect
            JSch jsch = new JSch();
            session = jsch.getSession(
                    partner.getSftpUsername(),
                    partner.getSftpHost(),
                    partner.getSftpPort() != null ? partner.getSftpPort() : 22);
            session.setPassword(partner.getSftpPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(connectionTimeout);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            // List files in inbound directory
            String inboundDir = partner.getSftpInboundDir();
            if (inboundDir == null || inboundDir.isEmpty()) {
                LOG.warn("No inbound directory configured for partner {}", partner.getPartnerId());
                return;
            }

            Vector<?> files = sftp.ls(inboundDir);
            int processed = 0;

            for (Object entry : files) {
                if (entry instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) entry;
                    String fileName = lsEntry.getFilename();

                    // Skip directories and hidden files
                    if (fileName.equals(".") || fileName.equals("..") || fileName.startsWith(".")) {
                        continue;
                    }
                    // Only process .edi, .x12, .txt files
                    if (!isEdiFile(fileName)) {
                        continue;
                    }

                    String filePath = inboundDir + "/" + fileName;
                    LOG.info("Processing SFTP file: {} from partner {}", fileName, partner.getPartnerId());

                    try {
                        // Read file content
                        String ediContent = readRemoteFile(sftp, filePath);
                        if (ediContent == null || ediContent.trim().isEmpty()) {
                            LOG.warn("Empty file skipped: {}", fileName);
                            continue;
                        }

                        // Detect transaction type using partner-aware delimiter resolution
                        EDIDelimiterDetector delimiters = EDIDelimiterDetector.detect(ediContent, partner);
                        String txnType = delimiters.detectTransactionType(ediContent);

                        // Resolve model class
                        Class<?> modelClass = modelRegistry.resolve(txnType);
                        if (modelClass == null) {
                            LOG.warn("Unknown transaction type '{}' in file {}", txnType, fileName);
                            loggingService.logFailure(ediContent, txnType,
                                    "Unknown transaction type: " + txnType,
                                    partner.getPartnerId(),
                                    EDITransactionLog.Source.SFTP, fileName);
                            continue;
                        }

                        // Parse with resolved delimiters (partner config Ã¢ÂÂ ISA header Ã¢ÂÂ defaults)
                        EDIUnmarshalResult<?> parseResult = EDIUnmarshaller.unmarshalResult(
                                modelClass, new StringReader(ediContent), delimiters.toMessageConfiguration());

                        // Generate 997 if partner wants auto-acknowledge
                        EDI997Generator.Result ackResult = null;
                        if (partner.isAutoAcknowledge()) {
                            ackResult = EDI997Generator.generate(parseResult);
                        }

                        // Log to database
                        EDITransactionLog txnLog = loggingService.logInboundTransaction(
                                ediContent, txnType, parseResult, ackResult,
                                partner.getPartnerId(),
                                EDITransactionLog.Source.SFTP, fileName);

                        // Deliver 997 to outbound directory
                        if (ackResult != null && ackResult.isSuccess()
                                && partner.getSftpOutboundDir() != null
                                && !partner.getSftpOutboundDir().isEmpty()) {
                            String ackFileName = "997_" + ackResult.getControlNumber() + ".edi";
                            String ackPath = partner.getSftpOutboundDir() + "/" + ackFileName;
                            writeRemoteFile(sftp, ackPath, ackResult.getEdiOutput());
                            loggingService.markAcknowledgementDelivered(txnLog.getTransactionId());
                            LOG.info("997 delivered to {}: {}", partner.getPartnerId(), ackFileName);
                        }

                        // Move original to archive
                        if (partner.getSftpArchiveDir() != null && !partner.getSftpArchiveDir().isEmpty()) {
                            ensureRemoteDir(sftp, partner.getSftpArchiveDir());
                            String archivePath = partner.getSftpArchiveDir() + "/" + fileName;
                            sftp.rename(filePath, archivePath);
                            LOG.info("Archived: {} -> {}", filePath, archivePath);
                        } else {
                            // No archive dir: delete the processed file
                            sftp.rm(filePath);
                            LOG.info("Removed processed file: {}", filePath);
                        }

                        processed++;

                    } catch (Exception e) {
                        LOG.error("Failed to process file {} from partner {}: {}",
                                fileName, partner.getPartnerId(), e.getMessage());
                        loggingService.logFailure(null, "UNKNOWN",
                                "File processing failed: " + e.getMessage(),
                                partner.getPartnerId(),
                                EDITransactionLog.Source.SFTP, fileName);
                    }
                }
            }

            LOG.info("SFTP poll complete for partner {}: {} file(s) processed",
                    partner.getPartnerId(), processed);

        } catch (Exception e) {
            LOG.error("SFTP connection failed for partner {}: {}", partner.getPartnerId(), e.getMessage());
        } finally {
            if (sftp != null && sftp.isConnected()) sftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // --- Helpers ---

    private boolean isEdiFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".edi") || lower.endsWith(".x12")
            || lower.endsWith(".txt") || lower.endsWith(".dat");
    }

    private String readRemoteFile(ChannelSftp sftp, String path) throws Exception {
        try (InputStream is = sftp.get(path);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void writeRemoteFile(ChannelSftp sftp, String path, String content) throws Exception {
        sftp.put(new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), path);
    }

    private void ensureRemoteDir(ChannelSftp sftp, String dir) {
        try {
            sftp.stat(dir);
        } catch (Exception e) {
            try {
                sftp.mkdir(dir);
            } catch (Exception e2) {
                LOG.debug("Directory might already exist: {}", dir);
            }
        }
    }
}
