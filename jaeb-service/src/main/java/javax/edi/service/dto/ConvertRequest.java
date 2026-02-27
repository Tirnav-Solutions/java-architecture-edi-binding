package javax.edi.service.dto;

/**
 * Request body for EDI format conversion.
 * Converts between delimiter formats, or re-serializes EDI with different delimiters.
 */
public class ConvertRequest {

    /** The raw EDI content to convert. */
    private String ediContent;

    /** The transaction set type key, e.g. "850". */
    private String transactionType;

    // --- Source delimiters (what the input uses) ---
    private Character sourceElementDelimiter;
    private Character sourceSegmentDelimiter;
    private Character sourceComponentDelimiter;

    // --- Target delimiters (what the output should use) ---
    private Character targetElementDelimiter;
    private Character targetSegmentDelimiter;
    private Character targetComponentDelimiter;

    public String getEdiContent() { return ediContent; }
    public void setEdiContent(String ediContent) { this.ediContent = ediContent; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Character getSourceElementDelimiter() { return sourceElementDelimiter; }
    public void setSourceElementDelimiter(Character v) { this.sourceElementDelimiter = v; }

    public Character getSourceSegmentDelimiter() { return sourceSegmentDelimiter; }
    public void setSourceSegmentDelimiter(Character v) { this.sourceSegmentDelimiter = v; }

    public Character getSourceComponentDelimiter() { return sourceComponentDelimiter; }
    public void setSourceComponentDelimiter(Character v) { this.sourceComponentDelimiter = v; }

    public Character getTargetElementDelimiter() { return targetElementDelimiter; }
    public void setTargetElementDelimiter(Character v) { this.targetElementDelimiter = v; }

    public Character getTargetSegmentDelimiter() { return targetSegmentDelimiter; }
    public void setTargetSegmentDelimiter(Character v) { this.targetSegmentDelimiter = v; }

    public Character getTargetComponentDelimiter() { return targetComponentDelimiter; }
    public void setTargetComponentDelimiter(Character v) { this.targetComponentDelimiter = v; }
}
