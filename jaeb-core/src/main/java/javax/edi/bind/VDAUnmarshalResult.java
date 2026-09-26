package javax.edi.bind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.util.EDIValidationError;

/**
 * Result wrapper for VDA fixed-width message unmarshalling.
 *
 * @param <T> the VDA message type
 */
public class VDAUnmarshalResult<T> {

    private T data;
    private int recordCount;
    private boolean parsed;
    private String parseError;
    private long parseTimeMillis;
    private List<EDIValidationError> errors = new ArrayList<>();
    private Map<String, Integer> recordStats = new LinkedHashMap<>();

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    public boolean isParsed() { return parsed; }
    public void setParsed(boolean parsed) { this.parsed = parsed; }

    public boolean isValid() { return parsed && !hasErrors() && parseError == null; }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public List<EDIValidationError> getErrors() { return errors; }

    public void addError(EDIValidationError error) { errors.add(error); }

    public String getParseError() { return parseError; }
    public void setParseError(String parseError) {
        this.parseError = parseError;
        this.parsed = false;
    }

    public long getParseTimeMillis() { return parseTimeMillis; }
    public void setParseTimeMillis(long v) { this.parseTimeMillis = v; }

    /** Record type -> count, e.g. {"511" -> 1, "512" -> 1, "513" -> 15, "519" -> 1}. */
    public Map<String, Integer> getRecordStats() { return recordStats; }
    public void setRecordStats(Map<String, Integer> recordStats) { this.recordStats = recordStats; }

    public String getErrorSummary() {
        StringBuilder sb = new StringBuilder();
        if (parseError != null) sb.append("Parse error: ").append(parseError).append("\n");
        for (EDIValidationError e : errors) sb.append(e.toString()).append("\n");
        return sb.toString();
    }
}
