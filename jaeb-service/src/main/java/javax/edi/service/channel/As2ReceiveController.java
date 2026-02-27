package javax.edi.service.channel;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AS2 receive endpoint.
 * 
 * AS2 is a push protocol â the trading partner sends EDI messages via HTTP POST
 * to this endpoint. This controller:
 * 
 * 1. Validates the AS2-From / AS2-To headers against configured channels
 * 2. Reads the EDI payload from the request body
 * 3. Parses, logs, and generates 997
 * 4. Returns an MDN (Message Disposition Notification) in the response
 * 
 * Endpoint: POST /as2/receive
 * 
 * Required headers (per RFC 4130):
 *   AS2-From: sender's AS2 ID
 *   AS2-To:   receiver's AS2 ID (our local ID)
 *   Message-ID: unique message identifier
 */
@RestController
@RequestMapping("/as2")
public class As2ReceiveController {

    private static final Logger LOG = LoggerFactory.getLogger(As2ReceiveController.class);

    @Autowired private CommunicationChannelRepository channelRepo;
    @Autowired private TradingPartnerRepository partnerRepo;
    @Autowired private EDIModelRegistry modelRegistry;
    @Autowired private TransactionLoggingService loggingService;
    @Autowired private As2CryptoService as2CryptoService;

    @PostMapping(value = "/receive", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> receiveAs2(
            @RequestBody byte[] body,
            @RequestHeader(value = "AS2-From", required = false) String as2From,
            @RequestHeader(value = "AS2-To", required = false) String as2To,
            @RequestHeader(value = "Message-ID", required = false) String messageId,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader HttpHeaders allHeaders) {

        LOG.info("AS2 message received: AS2-From={}, AS2-To={}, Message-ID={}, Content-Type={}, size={} bytes",
                as2From, as2To, messageId, contentType, body != null ? body.length : 0);

        // Validate required headers
        if (as2From == null || as2To == null) {
            LOG.warn("AS2 receive rejected: missing AS2-From or AS2-To header");
            return ResponseEntity.badRequest().body(buildMdn(
                    as2From, as2To, messageId, "failure",
                    "Missing required AS2-From or AS2-To header"));
        }

        // Find matching AS2 channel by partner's AS2 ID
        CommunicationChannel inboundChannel = findAs2Channel(as2From, as2To);
        String partnerId = null;

        if (inboundChannel != null) {
            partnerId = inboundChannel.getPartnerId();
            inboundChannel.setLastPolledAt(LocalDateTime.now());
            LOG.info("AS2 channel matched: {} (partner={})", inboundChannel.getChannelName(), partnerId);
        } else {
            LOG.warn("No AS2 channel found for AS2-From={}, AS2-To={}. Processing anyway.", as2From, as2To);
        }

        // S/MIME processing: decrypt and/or verify signature
        As2CryptoService.ProcessResult cryptoResult = as2CryptoService.processInbound(
                body, contentType, inboundChannel);

        if (cryptoResult.hasError()) {
            LOG.error("AS2 crypto processing error: {}", cryptoResult.getError());
            loggingService.logFailure(null, "UNKNOWN",
                    "AS2 crypto error: " + cryptoResult.getError(), partnerId,
                    EDITransactionLog.Source.BATCH, "AS2:" + messageId);

            return ResponseEntity.ok()
                    .header("Content-Type", "message/disposition-notification")
                    .body(buildMdn(as2From, as2To, messageId, "processed/error",
                            "Security processing failed: " + cryptoResult.getError()));
        }

        if (cryptoResult.isDecrypted()) {
            LOG.info("AS2 message decrypted successfully");
        }
        if (cryptoResult.isSignatureVerified()) {
            LOG.info("AS2 message signature verified (MIC algorithm: {})", cryptoResult.getMicAlgorithm());
        }

        // Parse EDI content from the (decrypted/verified) payload
        String ediContent = new String(cryptoResult.getPayload(), StandardCharsets.UTF_8);

        try {
            // Look up partner for delimiter config and auto-ack
            TradingPartner partner = partnerId != null
                    ? partnerRepo.findByPartnerId(partnerId).orElse(null) : null;

            // Detect transaction type using partner's delimiter config
            EDIDelimiterDetector delimiters = EDIDelimiterDetector.detect(ediContent, partner);
            String txnType = delimiters.detectTransactionType(ediContent);

            Class<?> modelClass = modelRegistry.resolve(txnType);
            if (modelClass == null) {
                loggingService.logFailure(ediContent, txnType,
                        "Unknown transaction type: " + txnType, partnerId,
                        EDITransactionLog.Source.BATCH, "AS2:" + messageId);

                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain")
                        .body(buildMdn(as2From, as2To, messageId, "processed/warning",
                                "Unknown transaction type: " + txnType));
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
                    ediContent, txnType, parseResult, ackResult, partnerId,
                    EDITransactionLog.Source.BATCH, "AS2:" + messageId);

            // Update channel status
            if (inboundChannel != null) {
                inboundChannel.setLastSuccessAt(LocalDateTime.now());
                inboundChannel.setUpdatedAt(LocalDateTime.now());
                channelRepo.save(inboundChannel);
            }

            // Build success MDN
            String disposition = parseResult.isParsed() ? "processed" : "processed/warning";
            String mdnText = buildMdn(as2From, as2To, messageId, disposition,
                    "Received and processed. Transaction=" + txnLog.getTransactionId()
                    + ", Type=" + txnType + ", Status=" + txnLog.getStatus());

            LOG.info("AS2 message processed: txn={}, type={}, status={}",
                    txnLog.getTransactionId(), txnType, txnLog.getStatus());

            return ResponseEntity.ok()
                    .header("Content-Type", "message/disposition-notification")
                    .header("AS2-From", as2To)
                    .header("AS2-To", as2From)
                    .body(mdnText);

        } catch (Exception e) {
            LOG.error("AS2 processing failed: {}", e.getMessage(), e);
            loggingService.logFailure(ediContent, "UNKNOWN",
                    "AS2 processing error: " + e.getMessage(), partnerId,
                    EDITransactionLog.Source.BATCH, "AS2:" + messageId);

            if (inboundChannel != null) {
                inboundChannel.setLastErrorAt(LocalDateTime.now());
                inboundChannel.setLastErrorMessage(e.getMessage());
                channelRepo.save(inboundChannel);
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "message/disposition-notification")
                    .body(buildMdn(as2From, as2To, messageId, "processed/error",
                            "Processing error: " + e.getMessage()));
        }
    }

    /**
     * Find an active AS2 inbound channel matching the sender's AS2 ID.
     */
    private CommunicationChannel findAs2Channel(String as2From, String as2To) {
        List<CommunicationChannel> channels = channelRepo.findByStatusAndDirectionAndProtocolIn(
                ChannelStatus.ACTIVE, Direction.INBOUND,
                java.util.Collections.singletonList(Protocol.AS2));

        for (CommunicationChannel ch : channels) {
            // Match: the partner's AS2 ID == AS2-From, our local ID == AS2-To
            if (as2From.equals(ch.getAs2PartnerId()) && as2To.equals(ch.getAs2LocalId())) {
                return ch;
            }
        }
        return null;
    }

    /**
     * Build a simple MDN (Message Disposition Notification) per RFC 3798.
     */
    private String buildMdn(String as2From, String as2To, String messageId,
                            String disposition, String text) {
        StringBuilder mdn = new StringBuilder();
        mdn.append("Reporting-UA: JAEB-EDI-Platform\r\n");
        if (messageId != null) {
            mdn.append("Original-Message-ID: ").append(messageId).append("\r\n");
        }
        mdn.append("Final-Recipient: rfc822; ").append(as2To != null ? as2To : "unknown").append("\r\n");
        mdn.append("Original-Recipient: rfc822; ").append(as2From != null ? as2From : "unknown").append("\r\n");
        mdn.append("Disposition: automatic-action/MDN-sent-automatically; ").append(disposition).append("\r\n");
        if (text != null) {
            mdn.append("\r\n").append(text).append("\r\n");
        }
        return mdn.toString();
    }

}
