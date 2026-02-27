package javax.edi.service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.entity.EDITransactionLog.Direction;
import javax.edi.service.entity.EDITransactionLog.Source;
import javax.edi.service.entity.EDITransactionLog.Status;
import javax.edi.service.repository.EDITransactionLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for logging all EDI transactions to the database.
 * Called by controllers after parse/generate/acknowledge operations.
 */
@Service
public class TransactionLoggingService {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionLoggingService.class);

    @Autowired
    private EDITransactionLogRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Logs an inbound EDI parse result to the database.
     *
     * @param rawEdi          the raw EDI content
     * @param transactionType the transaction type code (e.g. "850")
     * @param parseResult     the unmarshal result
     * @param ackResult       the 997 generation result (can be null)
     * @param partnerId       the trading partner ID (can be null for direct API)
     * @param source          the source (API, SFTP, BATCH)
     * @param sourceFileName  the source filename (for SFTP, can be null)
     * @return the saved transaction log entry
     */
    public EDITransactionLog logInboundTransaction(
            String rawEdi,
            String transactionType,
            EDIUnmarshalResult<?> parseResult,
            EDI997Generator.Result ackResult,
            String partnerId,
            Source source,
            String sourceFileName) {

        EDITransactionLog log = new EDITransactionLog();
        log.setTransactionId(UUID.randomUUID().toString());
        log.setDirection(Direction.INBOUND);
        log.setTransactionType(transactionType);
        log.setSource(source);
        log.setSourceFileName(sourceFileName);
        log.setPartnerId(partnerId);
        log.setRawEdiContent(rawEdi);
        log.setProcessedAt(LocalDateTime.now());

        if (parseResult != null) {
            log.setParsed(parseResult.isParsed());
            log.setValid(parseResult.isValid());
            log.setSegmentCount(parseResult.getSegmentCount());
            log.setParseTimeMs(parseResult.getParseTimeMillis());

            if (parseResult.getData() != null) {
                try {
                    log.setParsedJson(objectMapper.writeValueAsString(parseResult.getData()));
                } catch (Exception e) {
                    LOG.warn("Failed to serialize parsed data to JSON: {}", e.getMessage());
                }
            }

            if (parseResult.hasErrors()) {
                log.setValidationErrorCount(parseResult.getErrorCount());
                log.setValidationErrors(parseResult.getErrorSummary());
            }

            if (parseResult.getParseError() != null) {
                log.setErrorMessage(parseResult.getParseError());
            }

            // Extract envelope data
            extractEnvelopeData(log, parseResult);

            // Determine status
            if (!parseResult.isParsed()) {
                log.setStatus(Status.FAILED);
            } else if (parseResult.hasErrors()) {
                log.setStatus(Status.PARSED_WITH_ERRORS);
            } else {
                log.setStatus(Status.PARSED);
            }
        } else {
            log.setStatus(Status.FAILED);
        }

        // 997 acknowledgement
        if (ackResult != null && ackResult.isSuccess()) {
            log.setAcknowledgeCode(ackResult.getAcknowledgeCode());
            log.setEdi997Content(ackResult.getEdiOutput());
            log.setEdi997ControlNumber(ackResult.getControlNumber());
            log.setStatus(Status.ACKNOWLEDGED);
        }

        try {
            EDITransactionLog saved = repository.save(log);
            LOG.info("Transaction logged: id={}, txnId={}, type={}, partner={}, status={}",
                    saved.getId(), saved.getTransactionId(), transactionType, partnerId, saved.getStatus());
            return saved;
        } catch (Exception e) {
            LOG.error("Failed to save transaction log: {}", e.getMessage(), e);
            return log; // Return unsaved log so the API response isn't affected
        }
    }

    /**
     * Logs a failed processing attempt.
     */
    public EDITransactionLog logFailure(String rawEdi, String transactionType,
                                        String errorMessage, String partnerId,
                                        Source source, String sourceFileName) {
        EDITransactionLog log = new EDITransactionLog();
        log.setTransactionId(UUID.randomUUID().toString());
        log.setDirection(Direction.INBOUND);
        log.setTransactionType(transactionType != null ? transactionType : "UNKNOWN");
        log.setSource(source);
        log.setSourceFileName(sourceFileName);
        log.setPartnerId(partnerId);
        log.setRawEdiContent(rawEdi);
        log.setStatus(Status.ERROR);
        log.setErrorMessage(errorMessage);
        log.setProcessedAt(LocalDateTime.now());

        try {
            return repository.save(log);
        } catch (Exception e) {
            LOG.error("Failed to save error log: {}", e.getMessage());
            return log;
        }
    }

    /**
     * Marks a 997 as delivered.
     */
    public void markAcknowledgementDelivered(String transactionId) {
        repository.findByTransactionId(transactionId).ifPresent(log -> {
            log.setEdi997Delivered(true);
            log.setEdi997DeliveredAt(LocalDateTime.now());
            log.setStatus(Status.DELIVERED);
            repository.save(log);
            LOG.info("997 delivery confirmed for transaction {}", transactionId);
        });
    }

    /**
     * Extracts ISA/GS envelope data from the parse result via the data object.
     */
    private void extractEnvelopeData(EDITransactionLog log, EDIUnmarshalResult<?> parseResult) {
        if (parseResult.getData() == null) return;
        try {
            Object data = parseResult.getData();
            // Use the same reflection approach as EDI997Generator
            Object isa = getFieldValue(data, "envelopeHeader");
            if (isa != null) {
                log.setSenderIsaId(getStringField(isa, "interchangeSenderID"));
                log.setReceiverIsaId(getStringField(isa, "interchangeReceiverID"));
                log.setInterchangeControlNumber(getStringField(isa, "interchangeControlNumber"));
            }
            Object gs = getFieldValue(data, "groupEnvelopeHeader");
            if (gs != null) {
                log.setGroupControlNumber(getStringField(gs, "groupControlNumber"));
            }
        } catch (Exception e) {
            LOG.debug("Could not extract envelope data: {}", e.getMessage());
        }
    }

    private Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getStringField(Object obj, String fieldName) {
        Object val = getFieldValue(obj, fieldName);
        return val != null ? val.toString() : null;
    }
}
