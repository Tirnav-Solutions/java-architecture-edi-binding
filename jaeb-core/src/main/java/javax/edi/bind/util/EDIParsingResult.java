package javax.edi.bind.util;

import java.util.List;

/**
 * Result of unmarshalling an EDI message.
 * Contains the parsed object and any validation errors encountered.
 */
public class EDIParsingResult<T> {
    
    private T parsedObject;
    private EDIValidationError.ValidationResult validationResult;
    private boolean hasValidationErrors = false;
    
    public EDIParsingResult(T parsedObject, EDIValidationError.ValidationResult validationResult) {
        this.parsedObject = parsedObject;
        this.validationResult = validationResult;
        this.hasValidationErrors = validationResult != null && validationResult.hasErrors();
    }
    
    public T getParsedObject() {
        return parsedObject;
    }
    
    public EDIValidationError.ValidationResult getValidationResult() {
        return validationResult;
    }
    
    public List<EDIValidationError> getValidationErrors() {
        if (validationResult == null) {
            return java.util.Collections.emptyList();
        }
        return validationResult.getErrors();
    }
    
    public boolean hasValidationErrors() {
        return hasValidationErrors;
    }
    
    public int getValidationErrorCount() {
        if (validationResult == null) {
            return 0;
        }
        return validationResult.getErrorCount();
    }
    
    public String getErrorSummary() {
        if (validationResult == null) {
            return "No validation errors";
        }
        return validationResult.getErrorSummary();
    }
    
    public String getDetailedErrorSummary() {
        if (validationResult == null) {
            return "? No validation errors found";
        }
        return validationResult.getDetailedErrorSummary();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EDIParsingResult {\n");
        sb.append("  parsedObject: ").append(parsedObject != null ? parsedObject.getClass().getSimpleName() : "null").append("\n");
        sb.append("  validationErrors: ").append(getValidationErrorCount()).append("\n");
        if (hasValidationErrors) {
            sb.append("  errors:\n");
            for (EDIValidationError error : getValidationErrors()) {
                sb.append("    - ").append(error.toString()).append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
