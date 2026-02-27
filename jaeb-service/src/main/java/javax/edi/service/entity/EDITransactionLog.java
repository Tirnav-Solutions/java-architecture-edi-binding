package javax.edi.service.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Transaction log entry. Every EDI message processed by the system
 * is recorded here with its raw content, parsed JSON, validation result,
 * and 997 acknowledgement.
 */
@Entity
@Table(name = "edi_transaction_log", indexes = {
    @Index(name = "idx_txn_tenant", columnList = "tenantId"),
    @Index(name = "idx_txn_partner", columnList = "partnerId"),
    @Index(name = "idx_txn_type", columnList = "transactionType"),
    @Index(name = "idx_txn_status", columnList = "status"),
    @Index(name = "idx_txn_control", columnList = "interchangeControlNumber"),
    @Index(name = "idx_txn_date", columnList = "processedAt")
})
public class EDITransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant scope. */
    @Column(length = 50)
    private String tenantId;

    /** Unique tracking ID for this transaction (UUID). */
    @Column(nullable = false, unique = true, length = 50)
    private String transactionId;

    /** Partner ID (references TradingPartner.partnerId). Null if processed via direct API. */
    @Column(length = 50)
    private String partnerId;

    /** Direction: INBOUND (received from partner) or OUTBOUND (sent to partner). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction = Direction.INBOUND;

    /** Transaction set type code (e.g. "850", "810"). */
    @Column(nullable = false, length = 10)
    private String transactionType;

    /** Source of the message: API, SFTP, BATCH. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source = Source.API;

    /** Original filename if read from SFTP. */
    @Column(length = 500)
    private String sourceFileName;

    // --- ISA/GS envelope data (extracted from the message) ---

    @Column(length = 15)
    private String senderIsaId;

    @Column(length = 15)
    private String receiverIsaId;

    @Column(length = 9)
    private String interchangeControlNumber;

    @Column(length = 9)
    private String groupControlNumber;

    @Column(length = 9)
    private String transactionSetControlNumber;

    // --- Raw content ---

    /** The raw EDI content as received. */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String rawEdiContent;

    /** The parsed JSON representation. */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String parsedJson;

    // --- Processing result ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RECEIVED;

    private boolean parsed;
    private boolean valid;
    private int segmentCount;
    private long parseTimeMs;
    private int validationErrorCount;

    /** Validation error details (if any). */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String validationErrors;

    /** Processing error message (if failed). */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String errorMessage;

    // --- 997 Acknowledgement ---

    /** AK9 acknowledge code: A, E, P, R. */
    @Column(length = 1)
    private String acknowledgeCode;

    /** The generated 997 EDI content. */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String edi997Content;

    /** Control number of the generated 997. */
    @Column(length = 9)
    private String edi997ControlNumber;

    /** Whether the 997 was delivered to the partner. */
    private boolean edi997Delivered;

    // --- Timestamps ---

    @Column(nullable = false)
    private LocalDateTime processedAt;

    private LocalDateTime edi997DeliveredAt;

    // --- Enums ---

    public enum Direction { INBOUND, OUTBOUND }
    public enum Source { API, SFTP, BATCH }
    public enum Status {
        RECEIVED,          // Received but not yet processed
        PARSED,            // Successfully parsed
        PARSED_WITH_ERRORS,// Parsed but has validation errors
        FAILED,            // Parse failed entirely
        ACKNOWLEDGED,      // 997 generated
        DELIVERED,         // 997 delivered to partner
        ERROR              // System error during processing
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getSenderIsaId() { return senderIsaId; }
    public void setSenderIsaId(String senderIsaId) { this.senderIsaId = senderIsaId; }

    public String getReceiverIsaId() { return receiverIsaId; }
    public void setReceiverIsaId(String receiverIsaId) { this.receiverIsaId = receiverIsaId; }

    public String getInterchangeControlNumber() { return interchangeControlNumber; }
    public void setInterchangeControlNumber(String interchangeControlNumber) { this.interchangeControlNumber = interchangeControlNumber; }

    public String getGroupControlNumber() { return groupControlNumber; }
    public void setGroupControlNumber(String groupControlNumber) { this.groupControlNumber = groupControlNumber; }

    public String getTransactionSetControlNumber() { return transactionSetControlNumber; }
    public void setTransactionSetControlNumber(String transactionSetControlNumber) { this.transactionSetControlNumber = transactionSetControlNumber; }

    public String getRawEdiContent() { return rawEdiContent; }
    public void setRawEdiContent(String rawEdiContent) { this.rawEdiContent = rawEdiContent; }

    public String getParsedJson() { return parsedJson; }
    public void setParsedJson(String parsedJson) { this.parsedJson = parsedJson; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isParsed() { return parsed; }
    public void setParsed(boolean parsed) { this.parsed = parsed; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public int getSegmentCount() { return segmentCount; }
    public void setSegmentCount(int segmentCount) { this.segmentCount = segmentCount; }

    public long getParseTimeMs() { return parseTimeMs; }
    public void setParseTimeMs(long parseTimeMs) { this.parseTimeMs = parseTimeMs; }

    public int getValidationErrorCount() { return validationErrorCount; }
    public void setValidationErrorCount(int validationErrorCount) { this.validationErrorCount = validationErrorCount; }

    public String getValidationErrors() { return validationErrors; }
    public void setValidationErrors(String validationErrors) { this.validationErrors = validationErrors; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getAcknowledgeCode() { return acknowledgeCode; }
    public void setAcknowledgeCode(String acknowledgeCode) { this.acknowledgeCode = acknowledgeCode; }

    public String getEdi997Content() { return edi997Content; }
    public void setEdi997Content(String edi997Content) { this.edi997Content = edi997Content; }

    public String getEdi997ControlNumber() { return edi997ControlNumber; }
    public void setEdi997ControlNumber(String edi997ControlNumber) { this.edi997ControlNumber = edi997ControlNumber; }

    public boolean isEdi997Delivered() { return edi997Delivered; }
    public void setEdi997Delivered(boolean edi997Delivered) { this.edi997Delivered = edi997Delivered; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getEdi997DeliveredAt() { return edi997DeliveredAt; }
    public void setEdi997DeliveredAt(LocalDateTime edi997DeliveredAt) { this.edi997DeliveredAt = edi997DeliveredAt; }
}
