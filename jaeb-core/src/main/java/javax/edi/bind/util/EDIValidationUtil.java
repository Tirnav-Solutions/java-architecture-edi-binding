package javax.edi.bind.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;

import javax.edi.bind.EDIMessageException;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for validating EDI objects using JSR-303 validation annotations.
 * Supports validation of @NotNull, @Size, and other standard validation annotations.
 * 
 * Now supports two modes:
 * 1. Strict mode: Throws exception immediately on validation failure
 * 2. Lenient mode: Collects all validation errors and returns them
 */
public class EDIValidationUtil {
    
    private static final Logger LOG = LoggerFactory.getLogger(EDIValidationUtil.class);
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();
    
    private EDIValidationUtil() {
        // seal
    }
    
    /**
     * Validates the given object against its JSR-303 validation annotations (STRICT MODE).
     * Throws an exception immediately if validation fails.
     * 
     * @param <T> the type of object to validate
     * @param object the object to validate
     * @throws EDIMessageException if validation fails
     */
    public static <T> void validate(T object) throws EDIMessageException {
        if (object == null) {
            LOG.warn("Attempted to validate null object");
            return;
        }
        
        Set<ConstraintViolation<T>> violations;
        try {
            violations = validator.validate(object);
        } catch (javax.validation.UnexpectedTypeException e) {
            LOG.warn("Skipping validation for " + object.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }
        
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation failed for ").append(object.getClass().getSimpleName()).append(":\n");
            
            for (ConstraintViolation<T> violation : violations) {
                sb.append("  - ").append(violation.getPropertyPath())
                  .append(": ").append(violation.getMessage())
                  .append(" (invalid value: ").append(violation.getInvalidValue()).append(")")
                  .append("\n");
            }
            
            LOG.error(sb.toString());
            throw new EDIMessageException(sb.toString());
        }
    }
    
    /**
     * Validates the given object and collects all errors (LENIENT MODE).
     * Does NOT throw an exception. Returns validation result with all errors collected.
     * 
     * @param <T> the type of object to validate
     * @param object the object to validate
     * @return ValidationResult containing all validation errors
     */
    public static <T> EDIValidationError.ValidationResult validateAndCollectErrors(T object) {
        EDIValidationError.ValidationResult result = new EDIValidationError.ValidationResult();
        
        if (object == null) {
            LOG.warn("Attempted to validate null object");
            return result;
        }
        
        Set<ConstraintViolation<T>> violations;
        try {
            violations = validator.validate(object);
        } catch (javax.validation.UnexpectedTypeException e) {
            LOG.warn("Skipping validation for " + object.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
        
        for (ConstraintViolation<T> violation : violations) {
            String fieldPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            Object invalidValue = violation.getInvalidValue();
            
            result.addError(fieldPath, message, invalidValue);
        }
        
        if (result.hasErrors()) {
            LOG.error(result.getErrorSummary());
        }
        
        return result;
    }
    
    /**
     * Validates the given object and collects all errors with segment information.
     * 
     * @param <T> the type of object to validate
     * @param object the object to validate
     * @param segmentTag the EDI segment tag (e.g., "ST", "BEG", "CTT")
     * @param lineNumber the line number in the EDI message (optional, can be 0)
     * @return ValidationResult containing all validation errors with segment information
     */
    public static <T> EDIValidationError.ValidationResult validateAndCollectErrors(T object, String segmentTag, int lineNumber) {
        EDIValidationError.ValidationResult result = new EDIValidationError.ValidationResult();
        
        if (object == null) {
            LOG.warn("Attempted to validate null object");
            return result;
        }
        
        Set<ConstraintViolation<T>> violations;
        try {
            violations = validator.validate(object);
        } catch (javax.validation.UnexpectedTypeException e) {
            LOG.warn("Skipping validation for " + object.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
        
        for (ConstraintViolation<T> violation : violations) {
            String fieldPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            Object invalidValue = violation.getInvalidValue();
            
            result.addError(fieldPath, message, invalidValue, segmentTag, lineNumber);
        }
        
        if (result.hasErrors()) {
            LOG.error(result.getDetailedErrorSummary());
        }
        
        return result;
    }
    
    /**
     * Validates the given object without throwing an exception.
     * Returns true if validation passes, false otherwise.
     * 
     * @param <T> the type of object to validate
     * @param object the object to validate
     * @return true if validation passes, false otherwise
     */
    public static <T> boolean isValid(T object) {
        if (object == null) {
            return true;
        }
        
        try {
            Set<ConstraintViolation<T>> violations = validator.validate(object);
            return violations.isEmpty();
        } catch (javax.validation.UnexpectedTypeException e) {
            LOG.warn("Skipping validation for " + object.getClass().getSimpleName() + ": " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Gets validation violations for an object without throwing an exception.
     * 
     * @param <T> the type of object to validate
     * @param object the object to validate
     * @return a set of constraint violations, empty if valid
     */
    public static <T> Set<ConstraintViolation<T>> getViolations(T object) {
        if (object == null) {
            return java.util.Collections.emptySet();
        }
        
        try {
            return validator.validate(object);
        } catch (javax.validation.UnexpectedTypeException e) {
            LOG.warn("Skipping validation for " + object.getClass().getSimpleName() + ": " + e.getMessage());
            return java.util.Collections.emptySet();
        }
    }
    
    /**
     * Recursively validates the entire EDI object tree (message, segments, segment groups).
     * Walks through all fields and validates each segment/segment group individually,
     * collecting all errors. Throws EDIMessageException if any errors are found.
     * 
     * @param <T> the type of object to validate
     * @param object the root EDI message object
     * @throws EDIMessageException if any validation errors are found
     */
    public static <T> void validateDeep(T object) throws EDIMessageException {
        EDIValidationError.ValidationResult result = validateDeepAndCollectErrors(object);
        if (result.hasErrors()) {
            LOG.error(result.getErrorSummary());
            throw new EDIMessageException(result.getErrorSummary());
        }
    }
    
    /**
     * Recursively validates the entire EDI object tree and collects all errors (LENIENT).
     * Walks through all fields and validates each segment/segment group individually.
     * Does NOT throw an exception.
     * 
     * @param <T> the type of object to validate
     * @param object the root EDI message object
     * @return ValidationResult containing all validation errors from the entire tree
     */
    public static <T> EDIValidationError.ValidationResult validateDeepAndCollectErrors(T object) {
        EDIValidationError.ValidationResult result = new EDIValidationError.ValidationResult();
        if (object == null) {
            return result;
        }
        validateObjectRecursive(object, "", result);
        return result;
    }
    
    private static void validateObjectRecursive(Object object, String parentPath, EDIValidationError.ValidationResult result) {
        if (object == null) {
            return;
        }
        
        Class<?> clazz = object.getClass();
        
        // Determine segment tag for error reporting
        String segmentTag = null;
        if (clazz.isAnnotationPresent(EDISegment.class)) {
            segmentTag = clazz.getAnnotation(EDISegment.class).tag();
        } else if (clazz.isAnnotationPresent(EDISegmentGroup.class)) {
            segmentTag = clazz.getAnnotation(EDISegmentGroup.class).header();
        }
        
        // Validate this object's own constraints
        try {
            Set<ConstraintViolation<Object>> violations = validator.validate(object);
            for (ConstraintViolation<Object> violation : violations) {
                String fieldPath = violation.getPropertyPath().toString();
                String fullPath = parentPath.isEmpty() ? fieldPath : parentPath + "." + fieldPath;
                String className = clazz.getSimpleName();
                String errorMsg = className + "." + fieldPath + ": " + violation.getMessage();
                result.addError(fullPath, errorMsg, violation.getInvalidValue(), segmentTag, 0);
            }
        } catch (javax.validation.UnexpectedTypeException e) {
            // Misconfigured constraint (e.g. @Size on a Date field) - log warning and skip
            LOG.warn("Skipping validation for " + clazz.getSimpleName() + ": " + e.getMessage());
        }
        
        // Recurse into fields that are segments or segment groups
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(object);
                if (fieldValue == null) {
                    continue;
                }
                
                String fieldPath = parentPath.isEmpty() ? field.getName() : parentPath + "." + field.getName();
                
                if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    Collection<?> collection = (Collection<?>) fieldValue;
                    int index = 0;
                    for (Object item : collection) {
                        if (item != null && isEDIAnnotated(item.getClass())) {
                            validateObjectRecursive(item, fieldPath + "[" + index + "]", result);
                        }
                        index++;
                    }
                } else if (isEDIAnnotated(fieldValue.getClass())) {
                    validateObjectRecursive(fieldValue, fieldPath, result);
                }
            } catch (IllegalAccessException e) {
                LOG.warn("Could not access field: " + field.getName(), e);
            }
        }
    }
    
    private static boolean isEDIAnnotated(Class<?> clazz) {
        return clazz.isAnnotationPresent(EDISegment.class) || clazz.isAnnotationPresent(EDISegmentGroup.class);
    }
}

