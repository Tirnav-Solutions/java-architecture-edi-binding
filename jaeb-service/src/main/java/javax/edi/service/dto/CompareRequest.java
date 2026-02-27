package javax.edi.service.dto;

/**
 * Request body for comparing two EDI documents.
 */
public class CompareRequest {

    /** The transaction set type key, e.g. "850". */
    private String transactionType;

    /** First EDI document (the "baseline" / original). */
    private String ediContentA;

    /** Second EDI document (the "comparison" / revised). */
    private String ediContentB;

    /** Optional labels for the two documents. */
    private String labelA;
    private String labelB;

    /** Optional delimiter overrides. */
    private Character elementDelimiter;
    private Character segmentDelimiter;
    private Character componentDelimiter;

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getEdiContentA() { return ediContentA; }
    public void setEdiContentA(String ediContentA) { this.ediContentA = ediContentA; }

    public String getEdiContentB() { return ediContentB; }
    public void setEdiContentB(String ediContentB) { this.ediContentB = ediContentB; }

    public String getLabelA() { return labelA != null ? labelA : "Document A"; }
    public void setLabelA(String labelA) { this.labelA = labelA; }

    public String getLabelB() { return labelB != null ? labelB : "Document B"; }
    public void setLabelB(String labelB) { this.labelB = labelB; }

    public Character getElementDelimiter() { return elementDelimiter; }
    public void setElementDelimiter(Character v) { this.elementDelimiter = v; }

    public Character getSegmentDelimiter() { return segmentDelimiter; }
    public void setSegmentDelimiter(Character v) { this.segmentDelimiter = v; }

    public Character getComponentDelimiter() { return componentDelimiter; }
    public void setComponentDelimiter(Character v) { this.componentDelimiter = v; }
}
