package javax.edi.bind;

import java.util.List;

import javax.edi.bind.util.EDIValidationError;

/**
 * Generic result wrapper for EDI marshalling (Object ? EDI string).
 * Contains the generated EDI output, validation errors (if any), and segment count.
 * 
 * Usage:
 *   EDIMarshalResult result = EDIMarshaller.marshalResult(purchaseOrder);
 *   if (result.hasErrors()) {
 *       System.out.println(result.getErrorSummary());
 *   } else {
 *       String ediOutput = result.getEdiOutput();
 *   }
 */
public class EDIMarshalResult {

    private String ediOutput;
    private EDIValidationError.ValidationResult validationResult;
    private int segmentCount;
    private boolean success;
    private String error;

    public EDIMarshalResult() {
        this.validationResult = new EDIValidationError.ValidationResult();
        this.segmentCount = 0;
        this.success = false;
    }

    /**
     * Returns the generated EDI string. Null if marshalling failed.
     */
    public String getEdiOutput() {
        return ediOutput;
    }

    public void setEdiOutput(String ediOutput) {
        this.ediOutput = ediOutput;
    }

    /**
     * Returns true if EDI was generated successfully with no errors.
     */
    public boolean isSuccess() {
        return success && !hasErrors() && error == null;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
     * Returns the number of EDI segments generated.
     */
    public int getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(int segmentCount) {
        this.segmentCount = segmentCount;
    }

    /**
     * Returns the error message if marshalling failed. Null if successful.
     */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns a summary of all errors (marshalling + validation).
     */
    public String getErrorSummary() {
        StringBuilder sb = new StringBuilder();

        if (error != null) {
            sb.append("Marshal Error: ").append(error).append("\n");
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
        sb.append("EDIMarshalResult {");
        sb.append("\n  success=").append(isSuccess());
        sb.append(",\n  segmentCount=").append(segmentCount);

        if (error != null) {
            sb.append(",\n  error=").append(error);
        }

        if (hasErrors()) {
            sb.append(",\n  validationErrors=").append(getErrorCount());
            for (EDIValidationError err : getErrors()) {
                sb.append("\n    - ").append(err);
            }
        }

        if (ediOutput != null) {
            sb.append(",\n  outputLength=").append(ediOutput.length());
        }

        sb.append("\n}");
        return sb.toString();
    }
}
