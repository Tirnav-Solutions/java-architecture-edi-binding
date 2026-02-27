package javax.edi.service.dto;

/**
 * Request body for parsing (unmarshalling) raw EDI text into a JSON object.
 */
public class ParseRequest {

    /** The raw EDI content to parse (e.g. "ISA*00*...~GS*PO*...~"). */
    private String ediContent;

    /** The transaction set type key, e.g. "850", "PurchaseOrder". */
    private String transactionType;

    /** Optional: override the element delimiter (default from @EDIMessage). */
    private Character elementDelimiter;

    /** Optional: override the segment delimiter. */
    private Character segmentDelimiter;

    /** Optional: override the component delimiter. */
    private Character componentDelimiter;

    public String getEdiContent() {
        return ediContent;
    }

    public void setEdiContent(String ediContent) {
        this.ediContent = ediContent;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
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
