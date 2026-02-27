package javax.edi.service.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standard API response wrapper.
 * Every endpoint returns this shape so consuming systems can handle responses uniformly.
 * Includes requestId and timestamp for enterprise audit trail tracking.
 */
public class ApiResponse {

    private boolean success;
    private String message;
    private Object data;
    private List<String> errors;
    private Map<String, Object> metadata;
    private String requestId;
    private String timestamp;

    public ApiResponse() {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
    }

    public static ApiResponse ok(Object data) {
        ApiResponse r = new ApiResponse();
        r.success = true;
        r.data = data;
        return r;
    }

    public static ApiResponse ok(Object data, String message) {
        ApiResponse r = ok(data);
        r.message = message;
        return r;
    }

    public static ApiResponse ok(Object data, Map<String, Object> metadata) {
        ApiResponse r = ok(data);
        r.metadata = metadata;
        return r;
    }

    public static ApiResponse error(String message) {
        ApiResponse r = new ApiResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public static ApiResponse error(String message, List<String> errors) {
        ApiResponse r = error(message);
        r.errors = errors;
        return r;
    }

    // Getters & setters

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
