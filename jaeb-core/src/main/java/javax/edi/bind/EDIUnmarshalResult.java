package javax.edi.bind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.util.EDIValidationError;

/**
 * Generic result wrapper for EDI unmarshalling.
 * Contains the parsed object, validation errors (if any), segment count, and segment statistics.
 * 
 * Usage:
 *   EDIUnmarshalResult<PurchaseOrder> result = EDIUnmarshaller.unmarshalResult(PurchaseOrder.class, reader);
 *   if (result.hasErrors()) {
 *       System.out.println(result.getErrorSummary());
 *   } else {
 *       PurchaseOrder po = result.getData();
 *       System.out.println(result.getSegmentStats());  // {ISA=1, GS=1, ST=1, BEG=1, PO1=15, ...}
 *   }
 * 
 * @param <T> the EDI message type
 */
public class EDIUnmarshalResult<T> {

    private T data;
    private EDIValidationError.ValidationResult validationResult;
    private int segmentCount;
    private boolean parsed;
    private String parseError;
    private Map<String, Integer> segmentStats;
    private long parseTimeMillis;

    public EDIUnmarshalResult() {
        this.validationResult = new EDIValidationError.ValidationResult();
        this.segmentCount = 0;
        this.parsed = false;
        this.segmentStats = new LinkedHashMap<>();
    }

    /**
     * Returns the parsed EDI object. May be null if parsing failed entirely.
     */
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * Returns true if the EDI message was successfully parsed (object created).
     * Note: parsed can be true even if there are validation errors.
     */
    public boolean isParsed() {
        return parsed;
    }

    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }

    /**
     * Returns true if the EDI message is valid (parsed successfully with no validation errors).
     */
    public boolean isValid() {
        return parsed && !hasErrors() && parseError == null;
    }

    /**
     * Returns true if there are validation errors.
     */
    public boolean hasErrors() {
        return validationResult.hasErrors();
    }

    /**
     * Returns the list of validation errors.
     */
    public List<EDIValidationError> getErrors() {
        return validationResult.getErrors();
    }

    /**
     * Returns the number of validation errors.
     */
    public int getErrorCount() {
        return validationResult.getErrorCount();
    }

    /**
     * Returns the validation result object.
     */
    public EDIValidationError.ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(EDIValidationError.ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    /**
     * Returns the number of EDI segments parsed.
     */
    public int getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(int segmentCount) {
        this.segmentCount = segmentCount;
    }

    /**
     * Returns the parse error message if parsing failed entirely. Null if parsing succeeded.
     */
    public String getParseError() {
        return parseError;
    }

    public void setParseError(String parseError) {
        this.parseError = parseError;
    }

    /**
     * Returns segment type counts (e.g., {ISA=1, GS=1, ST=1, BEG=1, PO1=15}).
     */
    public Map<String, Integer> getSegmentStats() {
        return segmentStats;
    }

    public void setSegmentStats(Map<String, Integer> segmentStats) {
        this.segmentStats = segmentStats;
    }

    /**
     * Records a segment occurrence for statistics.
     */
    public void recordSegment(String segmentTag) {
        segmentStats.merge(segmentTag, 1, Integer::sum);
    }

    /**
     * Returns the time taken to parse the EDI message in milliseconds.
     */
    public long getParseTimeMillis() {
        return parseTimeMillis;
    }

    public void setParseTimeMillis(long parseTimeMillis) {
        this.parseTimeMillis = parseTimeMillis;
    }

    /**
     * Returns a summary of all errors (parse + validation).
     */
    public String getErrorSummary() {
        StringBuilder sb = new StringBuilder();

        if (parseError != null) {
            sb.append("Parse Error: ").append(parseError).append("\n");
        }

        if (hasErrors()) {
            sb.append(validationResult.getErrorSummary());
        }

        if (sb.length() == 0) {
            return "No errors";
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EDIUnmarshalResult {");
        sb.append("\n  parsed=").append(parsed);
        sb.append(",\n  valid=").append(isValid());
        sb.append(",\n  segmentCount=").append(segmentCount);
        sb.append(",\n  parseTimeMs=").append(parseTimeMillis);
        sb.append(",\n  type=").append(data != null ? data.getClass().getSimpleName() : "null");

        if (!segmentStats.isEmpty()) {
            sb.append(",\n  segmentStats=").append(segmentStats);
        }

        if (parseError != null) {
            sb.append(",\n  parseError=").append(parseError);
        }

        if (hasErrors()) {
            sb.append(",\n  validationErrors=").append(getErrorCount());
            for (EDIValidationError error : getErrors()) {
                sb.append("\n    - ").append(error);
            }
        }

        sb.append("\n}");
        return sb.toString();
    }
}
