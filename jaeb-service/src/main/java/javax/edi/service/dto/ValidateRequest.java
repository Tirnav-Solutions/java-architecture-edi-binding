package javax.edi.service.dto;

import java.util.Map;

/**
 * Request body for validating either raw EDI text or a JSON payload.
 * Supply either ediContent or payload (not both).
 */
public class ValidateRequest {

    /** The transaction set type key, e.g. "850", "PurchaseOrder". */
    private String transactionType;

    /** Raw EDI content to validate (parse + validate). */
    private String ediContent;

    /** JSON payload to validate (deserialize + validate). */
    private Map<String, Object> payload;

    /** Optional delimiter overrides. */
    private Character elementDelimiter;
    private Character segmentDelimiter;
    private Character componentDelimiter;

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getEdiContent() {
        return ediContent;
    }

    public void setEdiContent(String ediContent) {
        this.ediContent = ediContent;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Character getElementDelimiter() {
        return elementDelimiter;
    }

    public void setElementDelimiter(Character elementDelimiter) {
        this.elementDelimiter = elementDelimiter;
    }

    public Character getSegmentDelimiter() {
        return segmentDelimiter;
    }

    public void setSegmentDelimiter(Character segmentDelimiter) {
        this.segmentDelimiter = segmentDelimiter;
    }

    public Character getComponentDelimiter() {
        return componentDelimiter;
    }

    public void setComponentDelimiter(Character componentDelimiter) {
        this.componentDelimiter = componentDelimiter;
    }
}
