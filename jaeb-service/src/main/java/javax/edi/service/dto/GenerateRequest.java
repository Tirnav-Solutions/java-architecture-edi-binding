package javax.edi.service.dto;

import java.util.Map;

/**
 * Request body for generating (marshalling) a JSON object into raw EDI text.
 */
public class GenerateRequest {

    /** The transaction set type key, e.g. "850", "PurchaseOrder". */
    private String transactionType;

    /** The JSON payload representing the EDI message object. */
    private Map<String, Object> payload;

    /** Optional: override the element delimiter (default from @EDIMessage). */
    private Character elementDelimiter;

    /** Optional: override the segment delimiter. */
    private Character segmentDelimiter;

    /** Optional: override the component delimiter. */
    private Character componentDelimiter;

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
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
