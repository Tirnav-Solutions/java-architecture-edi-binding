package javax.edi.service.dto;

import java.util.List;

/**
 * Request body for batch parsing multiple EDI documents at once.
 */
public class BatchParseRequest {

    /** The transaction set type key, e.g. "850". All items must be the same type. */
    private String transactionType;

    /** List of raw EDI documents to parse. */
    private List<String> ediContents;

    /** Optional delimiter overrides applied to all documents. */
    private Character elementDelimiter;
    private Character segmentDelimiter;
    private Character componentDelimiter;

    /** If true, continue parsing remaining documents even if one fails. Default true. */
    private Boolean continueOnError;

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public List<String> getEdiContents() { return ediContents; }
    public void setEdiContents(List<String> ediContents) { this.ediContents = ediContents; }

    public Character getElementDelimiter() { return elementDelimiter; }
    public void setElementDelimiter(Character v) { this.elementDelimiter = v; }

    public Character getSegmentDelimiter() { return segmentDelimiter; }
    public void setSegmentDelimiter(Character v) { this.segmentDelimiter = v; }

    public Character getComponentDelimiter() { return componentDelimiter; }
    public void setComponentDelimiter(Character v) { this.componentDelimiter = v; }

    public boolean isContinueOnError() {
        return continueOnError == null || continueOnError;
    }
    public void setContinueOnError(Boolean continueOnError) { this.continueOnError = continueOnError; }
}
