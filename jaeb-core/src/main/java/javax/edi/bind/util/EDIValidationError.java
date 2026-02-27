package javax.edi.bind.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a validation error in an EDI message.
 * Collects all validation errors without stopping the parsing process.
 */
public class EDIValidationError {
    
    private String fieldPath;
    private String message;
    private Object invalidValue;
    private String segmentTag;
    private int lineNumber;
    
    public EDIValidationError(String fieldPath, String message, Object invalidValue) {
        this.fieldPath = fieldPath;
        this.message = message;
        this.invalidValue = invalidValue;
    }
    
    public EDIValidationError(String fieldPath, String message, Object invalidValue, String segmentTag, int lineNumber) {
        this.fieldPath = fieldPath;
        this.message = message;
        this.invalidValue = invalidValue;
        this.segmentTag = segmentTag;
        this.lineNumber = lineNumber;
    }
    
    public String getFieldPath() {
        return fieldPath;
    }
    
    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getInvalidValue() {
        return invalidValue;
    }
    
    public void setInvalidValue(Object invalidValue) {
        this.invalidValue = invalidValue;
    }
    
    public String getSegmentTag() {
        return segmentTag;
    }
    
    public void setSegmentTag(String segmentTag) {
        this.segmentTag = segmentTag;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (segmentTag != null) {
            sb.append("[").append(segmentTag).append("] ");
        }
        if (lineNumber > 0) {
            sb.append("Line ").append(lineNumber).append(": ");
        }
        sb.append(fieldPath).append(" - ").append(message);
        if (invalidValue != null) {
            sb.append(" (invalid value: ").append(invalidValue).append(")");
        }
        return sb.toString();
    }
    
    /**
     * Container for holding validation errors collected during parsing
     */
    public static class ValidationResult {
        private List<EDIValidationError> errors = new ArrayList<>();
        private boolean hasErrors = false;
        
        public void addError(EDIValidationError error) {
            errors.add(error);
            hasErrors = true;
        }
        
        public void addError(String fieldPath, String message, Object invalidValue) {
            errors.add(new EDIValidationError(fieldPath, message, invalidValue));
            hasErrors = true;
        }
        
        public void addError(String fieldPath, String message, Object invalidValue, String segmentTag, int lineNumber) {
            errors.add(new EDIValidationError(fieldPath, message, invalidValue, segmentTag, lineNumber));
            hasErrors = true;
        }
        
        public List<EDIValidationError> getErrors() {
            return errors;
        }
        
        public boolean hasErrors() {
            return hasErrors;
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        public String getErrorSummary() {
            if (!hasErrors) {
                return "No validation errors";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Validation failed with ").append(errors.size()).append(" error(s):\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i).toString()).append("\n");
            }
            return sb.toString();
        }
        
        public String getDetailedErrorSummary() {
            if (!hasErrors) {
                return "? No validation errors found";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("??????????????????????????????????????????????????????????????\n");
            sb.append("?  VALIDATION ERRORS - ").append(String.format("%d error(s)", errors.size())).append("\n");
            sb.append("??????????????????????????????????????????????????????????????\n");
            
            for (int i = 0; i < errors.size(); i++) {
                EDIValidationError error = errors.get(i);
                sb.append("? ").append(i + 1).append(". Field: ").append(error.getFieldPath()).append("\n");
                sb.append("?    Message: ").append(error.getMessage()).append("\n");
                if (error.getSegmentTag() != null) {
                    sb.append("?    Segment: ").append(error.getSegmentTag());
                    if (error.getLineNumber() > 0) {
                        sb.append(" (Line ").append(error.getLineNumber()).append(")");
                    }
                    sb.append("\n");
                }
                if (error.getInvalidValue() != null) {
                    sb.append("?    Value: ").append(error.getInvalidValue()).append("\n");
                }
            }
            
            sb.append("??????????????????????????????????????????????????????????????\n");
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return getErrorSummary();
        }
    }
}
