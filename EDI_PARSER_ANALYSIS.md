# EDI Parser and Generator - Code Analysis & Suggested Changes

## Current Issues Identified

### 1. **Missing Validation Support**
The current EDI parser/generator does NOT validate any annotations like:
- `@NotNull` 
- `@Size(min=1)`
- Other JSR-303 validation annotations

**Location:** 
- `EDIMarshaller.java` - Lines 30-200+ (marshalling/generation)
- `EDIUnmarshaller.java` - Lines 70-350+ (unmarshalling/parsing)

**Current Behavior:**
- The code ignores all validation annotations
- Null values are silently skipped (line 76 in EDIMarshaller: `if(fieldObj==null)`)
- No validation errors are thrown if required fields are missing
- Collections without minimum size requirements are accepted

**Example in PurchaseOrder.java:**
```java
@NotNull
@Size(min=1)
@EDICollectionType(PurchaseOrderBody.class)
private Collection<IEDIMessageBody<?>> body;
```
Currently, if `body` is null or empty, NO error is raised.

---

### 2. **Multiple Body Elements Not Parsing Correctly**
The current parser doesn't properly handle multiple body elements in a collection.

**Location:** 
- `EDIUnmarshaller.java` - Lines 155-175 (processSegmentGroup method)

**Current Issue:**
- When parsing segment groups with collections (line 159), the loop terminates prematurely
- The collection parsing logic (lines 162-176) has complex logic to detect when to stop reading
- For HL segments with hierarchy, there's special handling (lines 167-173) but this may not work correctly for all cases

**Problem Code:**
```java
do {
    EDIUnmarshaller.LOG.debug("Looping to collect Collection of Segment Groups");
    final Field[] fields = segmentGroupClass.getDeclaredFields();
    final Iterator<Field> fieldIterator = Arrays.asList(fields).iterator();
    final Object collectionObj = segmentGroupClass.newInstance();
    while (fieldIterator.hasNext() && segmentIterator.hasNext()) {
        parseEDISegmentOrSegmentGroup(ediMessage, collectionObj, fieldIterator, segmentIterator, hierarchy);
    }
    obj.add(collectionObj);
    final String nextLine = segmentIterator.peek();
    final String candidateTag = StringUtils.substringBefore(nextLine, CharUtils.toString(ediMessage.elementDelimiter()));
    if (!StringUtils.equals(segmentTag, candidateTag)) {
        break;  // <-- This may break too early
    }
    // ... HL segment special handling
} while (segmentIterator.hasNext());
```

---

## Suggested Changes

### **CHANGE 1: Add Validation Support**

**Files to Modify:**
1. `EDIMarshaller.java` 
2. `EDIUnmarshaller.java`
3. Create a new utility class: `EDIValidationUtil.java`

**Details:**

#### A. Create `EDIValidationUtil.java`
A new utility class that uses the Java Validation API (JSR-303) to validate objects:

```java
package javax.edi.bind.util;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.Validation;
import java.util.Set;

public class EDIValidationUtil {
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();
    
    public static <T> void validate(T object) throws EDIMessageException {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath())
                  .append(": ")
                  .append(violation.getMessage())
                  .append("\n");
            }
            throw new EDIMessageException("Validation failed:\n" + sb.toString());
        }
    }
}
```

#### B. Modify `EDIMarshaller.java`
- Add validation BEFORE marshalling each object
- Add validation check in `processSegmentsAndSegmentGroups()` method (line ~70)
- Check for null fields marked with `@NotNull`
- Check collection sizes marked with `@Size`

**Key Changes:**
```java
protected static <T> void processSegmentsAndSegmentGroups(EDIMessage message, T obj, Writer writer) throws Exception {
    // ADD VALIDATION HERE
    EDIValidationUtil.validate(obj);
    
    Class<?> clazz = obj.getClass();
    // ... rest of code
}
```

#### C. Modify `EDIUnmarshaller.java`
- Add validation AFTER unmarshalling each object
- Add validation check in `parseEDIMessage()` method (line ~85)
- Add validation check after setting field values in `parseEDISegmentFields()` (line ~315)
- Add validation check after completing segment group parsing in `processSegmentGroup()` (line ~200)

**Key Changes:**
```java
private static <T> T parseEDIMessage(final Class<T> clz, final Reader reader, final EDIMessage ediMessage) 
    throws InstantiationException, IllegalAccessException, ... {
    // ... existing code ...
    final T obj = clz.newInstance();
    final Stack<HierarchyReference> stack = new Stack<HierarchyReference>();
    while (fieldIterator.hasNext() && bufferedIterator.hasNext()) {
        parseEDISegmentOrSegmentGroup(ediMessage, obj, fieldIterator, bufferedIterator, stack);
    }
    
    // ADD VALIDATION HERE
    EDIValidationUtil.validate(obj);
    
    return obj;
}
```

---

### **CHANGE 2: Fix Multiple Body Parsing**

**File to Modify:** 
- `EDIUnmarshaller.java` - `processSegmentGroup()` method (lines 155-200)

**Root Cause:**
The current logic breaks when it encounters a segment that doesn't match the expected collection type. For multiple body elements, the parser needs to:
1. Keep reading segments as long as they match the segment group type
2. Only stop when it encounters a DIFFERENT segment type
3. Handle the @EDISegmentGroup marker properly

**Suggested Fix:**
Replace the loop breaking logic with more robust segment matching:

```java
if (Collection.class.isAssignableFrom(fm.getField().getType())) {
    final Collection obj = CollectionFactory.newInstance(fm.getField().getType());
    BeanUtils.setProperty((Object)object, fm.getField().getName(), (Object)obj);
    final String segmentTag = getSegmentTag(fm.getField(), true);
    
    do {
        EDIUnmarshaller.LOG.debug("Looping to collect Collection of Segment Groups");
        final Field[] fields = segmentGroupClass.getDeclaredFields();
        final Iterator<Field> fieldIterator = Arrays.asList(fields).iterator();
        final Object collectionObj = segmentGroupClass.newInstance();
        
        // Parse all fields in this segment group instance
        while (fieldIterator.hasNext() && segmentIterator.hasNext()) {
            parseEDISegmentOrSegmentGroup(ediMessage, collectionObj, fieldIterator, segmentIterator, hierarchy);
        }
        
        // ADD VALIDATION HERE
        EDIValidationUtil.validate(collectionObj);
        
        obj.add(collectionObj);
        
        // Check if next segment matches this collection's type
        if (!segmentIterator.hasNext()) {
            break;  // No more segments
        }
        
        final String nextLine = segmentIterator.peek();
        final String candidateTag = StringUtils.substringBefore(nextLine, CharUtils.toString(ediMessage.elementDelimiter()));
        
        // IMPROVED: Use matchesSegment() to check if next line matches this segment group
        if (!matchesSegment(fm.getField(), candidateTag)) {
            break;  // Different segment type found, stop collecting
        }
        
    } while (segmentIterator.hasNext());
}
```

---

## Implementation Steps

### **Phase 1: Validation Support**
1. Create `EDIValidationUtil.java`
2. Add `validate()` calls in `EDIMarshaller.processSegmentsAndSegmentGroups()`
3. Add `validate()` calls in `EDIUnmarshaller.parseEDIMessage()`
4. Add `validate()` calls after segment group parsing
5. Test with PurchaseOrder model

### **Phase 2: Fix Multiple Body Parsing**
1. Refactor collection parsing loop in `EDIUnmarshaller.processSegmentGroup()`
2. Replace complex HL-specific logic with general `matchesSegment()` approach
3. Remove the special HL handling (lines 167-173) if it conflicts with new logic
4. Test with multi-body PO messages

### **Phase 3: Testing**
1. Create test cases for PurchaseOrder with multiple bodies
2. Create test cases for missing required fields
3. Create test cases for Size validation
4. Verify backward compatibility with existing single-body messages

---

## Files That Will Be Modified

1. **NEW:** `javax/edi/bind/util/EDIValidationUtil.java`
2. **MODIFY:** `javax/edi/bind/EDIMarshaller.java`
3. **MODIFY:** `javax/edi/bind/EDIUnmarshaller.java`
4. **MODIFY:** `javax/edi/bind/EDIMessageException.java` (if needed for better error messages)

---

## Dependencies

- Java Validation API (JSR-303) - Already in pom.xml: `javax.validation:validation-api:1.1.0.Final`
- Hibernate Validator (for implementation) - May need to add to pom.xml if not present

---

## Benefits

? **Validation Support**: Enforce @NotNull, @Size, and other JSR-303 constraints  
? **Better Error Messages**: Clear indication of which field failed validation  
? **Correct Multi-Body Parsing**: Properly parse EDI messages with multiple body segments  
? **Backward Compatible**: Existing code will continue to work  
? **Extensible**: Easy to add more validation rules in the future  

---
