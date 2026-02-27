# Lenient Validation - Quick Reference

## Two Validation Modes

### Mode 1: STRICT (Default - Throws Exception)
```java
try {
    EDIMarshaller.marshal(message, writer);
    // or
    message = EDIUnmarshaller.unmarshal(Class.class, reader);
} catch (EDIMessageException e) {
    // Handle validation error
}
```

**Behavior**: Throws exception on first validation error  
**Use Case**: Production code, critical validations

---

### Mode 2: LENIENT (New - Collects Errors)
```java
// Without segment information
EDIValidationError.ValidationResult result = 
    FieldAwareConverter.validateObjectLenient(message);

// With segment information (tag and line number)
EDIValidationError.ValidationResult result = 
    FieldAwareConverter.validateObjectLenient(message, "ST", 5);
```

**Behavior**: Collects all validation errors, doesn't throw  
**Use Case**: Development, testing, error reporting, EDI debugging

---

## Checking for Errors

### Quick Check
```java
if (result.hasErrors()) {
    // There are validation errors
}
```

### Get Error Count
```java
int count = result.getErrorCount();
System.out.println("Found " + count + " error(s)");
```

### Print All Errors
```java
System.out.println(result.getDetailedErrorSummary());
```

### Iterate Through Errors
```java
for (EDIValidationError error : result.getErrors()) {
    String path = error.getFieldPath();      // e.g., "body"
    String msg = error.getMessage();         // e.g., "size must be at least 1"
    Object val = error.getInvalidValue();    // e.g., []
    String tag = error.getSegmentTag();      // e.g., "ST"
    int line = error.getLineNumber();        // e.g., 5
    
    System.out.println(path + ": " + msg);
}
```

---

## Complete Example

```java
import javax.edi.bind.util.FieldAwareConverter;
import javax.edi.bind.util.EDIValidationError;

public class EDIValidator {
    
    public void validateMessage(PurchaseOrder message) {
        // Use lenient mode to collect all errors
        EDIValidationError.ValidationResult result = 
            FieldAwareConverter.validateObjectLenient(message);
        
        // Check if there are any errors
        if (!result.hasErrors()) {
            System.out.println("? Message is valid");
            return;
        }
        
        // Print formatted error summary
        System.out.println(result.getDetailedErrorSummary());
        
        // Log each error individually
        System.out.println("\nDetailed Error List:");
        int errorNum = 1;
        for (EDIValidationError error : result.getErrors()) {
            System.out.printf("%d. Field: %s%n", errorNum, error.getFieldPath());
            System.out.printf("   Message: %s%n", error.getMessage());
            System.out.printf("   Value: %s%n", error.getInvalidValue());
            errorNum++;
        }
    }
}
```

---

## Real-World Scenario

### Scenario: Parsing EDI with Missing Required Fields

```java
// Parse EDI file
PurchaseOrder po = EDIUnmarshaller.unmarshal(PurchaseOrder.class, reader);

// Validate (lenient mode)
EDIValidationError.ValidationResult validation = 
    FieldAwareConverter.validateObjectLenient(po, "ST", 2);

// Report findings
if (validation.hasErrors()) {
    System.err.println("? EDI Message has " + validation.getErrorCount() + 
                      " validation error(s):");
    System.err.println(validation.getDetailedErrorSummary());
    
    // Could also process each error
    for (EDIValidationError error : validation.getErrors()) {
        logger.error("Segment [{}] - {}: {}", 
                    error.getSegmentTag(),
                    error.getFieldPath(), 
                    error.getMessage());
    }
} else {
    System.out.println("? EDI Message is valid");
}
```

---

## Error Summary Format

The `getDetailedErrorSummary()` output looks like:

```
? No validation errors found

OR

??????????????????????????????????????????????????????????????
?  VALIDATION ERRORS - 2 error(s)                           ?
??????????????????????????????????????????????????????????????
? 1. Field: envelopeHeader                                   ?
?    Message: may not be null                                ?
?    Value: null                                             ?
? 2. Field: body                                             ?
?    Message: size must be between 1 and 2147483647          ?
?    Value: []                                               ?
??????????????????????????????????????????????????????????????
```

---

## API Reference

### `EDIValidationError`
```java
// Properties
String getFieldPath()          // e.g., "body"
String getMessage()            // e.g., "size must be at least 1"
Object getInvalidValue()       // e.g., []
String getSegmentTag()         // e.g., "ST"
int getLineNumber()            // e.g., 5
```

### `EDIValidationError.ValidationResult`
```java
// Check for errors
boolean hasErrors()            // true if errors exist
int getErrorCount()            // number of errors

// Get errors
List<EDIValidationError> getErrors()

// Print errors
String getErrorSummary()       // Simple text summary
String getDetailedErrorSummary() // Formatted box summary
```

### `FieldAwareConverter`
```java
// Lenient validation (no exception thrown)
static ValidationResult validateObjectLenient(Object obj)
static ValidationResult validateObjectLenient(Object obj, String tag, int line)

// Strict validation (throws exception)
static void validateObject(Object obj)  // throws EDIMessageException
```

---

## Common Validation Annotations

```java
@NotNull              // Field cannot be null
@Size(min=1)          // Collection size >= 1
@Size(min=1, max=10)  // Collection size between 1-10
@Min(0)               // Number >= 0
@Max(100)             // Number <= 100
@Pattern(regexp="...")  // String matches pattern
@Email                // Valid email format
@Positive             // Number > 0
@Negative             // Number < 0
```

---

## Tips

1. **Use Lenient Mode for Debugging**
   ```java
   result = validateObjectLenient(obj);
   System.out.println(result.getDetailedErrorSummary());
   ```

2. **Use Strict Mode for Production**
   ```java
   try {
       EDIMarshaller.marshal(obj, writer);
   } catch (EDIMessageException e) {
       // Handle error
   }
   ```

3. **Add Segment Context When Available**
   ```java
   result = validateObjectLenient(obj, "ST", lineNumber);
   ```

4. **Log Validation Errors**
   ```java
   if (result.hasErrors()) {
       logger.error(result.getDetailedErrorSummary());
   }
   ```

---

**Lenient Validation Ready to Use!** ?
