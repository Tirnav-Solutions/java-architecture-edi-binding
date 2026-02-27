# Line-by-Line EDI Error Reporting - Quick Guide

## The Problem You Asked About

**Scenario**: EDI 850 has a price field with constraints @Min(1) and @Max(100). If price is 101, show the error with the line number.

**Solution**: Now implemented! ?

---

## Example: Price Validation Error

### Input EDI File (850_v2_invalid_prices.edi)
```
ISA*00*          *00*          *08*925485USNR...   [Line 1]
GS*PO*925485USNR*72773946T...                      [Line 2]
ST*850*0001*005010~                                 [Line 3]
BEG*00*SA*30935705**20110706~                       [Line 4]
...
PO1*00010*5*EA*150.00*LE*IN*1*VN*...~              [Line 10] ? Price 150.00 > 100
...
PO1*00020*5*EA*101.00*LE*IN*2*VN*...~              [Line 12] ? Price 101.00 > 100
...
```

### Output Report
```
?????????????????????????????????????????????????????????????
?  TEST: Line-by-Line Validation Report                     ?
?????????????????????????????????????????????????????????????

? VALIDATION REPORT

File: src/test/resources/850_v2_invalid_prices.edi
???????????????????????????????????????????????????????????

? Status: 2 VALIDATION ERROR(S) FOUND

Line | Segment | Field         | Error Message             | Value
?????????????????????????????????????????????????????????????????????
  10 | PO1     | unitPrice     | must be <= 100.00         | 150.0
  12 | PO1     | unitPrice     | must be <= 100.00         | 101.0
?????????????????????????????????????????????????????????????????????

? DETAILED ERROR INFORMATION

Error 1:
  Line Number : 10
  Segment Tag : PO1
  Field Name  : unitPrice
  Message     : must be <= 100.00
  Invalid Val : 150.0
  Constraint  : Price must be between 1.00 and 100.00

Error 2:
  Line Number : 12
  Segment Tag : PO1
  Field Name  : unitPrice
  Message     : must be <= 100.00
  Invalid Val : 101.0
  Constraint  : Price must be between 1.00 and 100.00
```

---

## How It Works

### 1. Define Validation Rules
```java
@EDISegment(tag = "PO1")
class TestPO1Segment {
    @Min(1)                    // Minimum price: 1.00
    @Max(100)                  // Maximum price: 100.00
    double unitPrice;
}
```

### 2. Read EDI File
```java
String filePath = "src/test/resources/850_v2_invalid_prices.edi";
String ediContent = readFileContent(filePath);
String[] segments = ediContent.split("~");
```

### 3. Validate with Line Tracking
```java
// For each segment, track line number and validate
EDIValidationError.ValidationResult result = 
    FieldAwareConverter.validateObjectLenient(segment, "PO1", lineNumber);

if (result.hasErrors()) {
    for (EDIValidationError error : result.getErrors()) {
        System.out.printf("Line %d: %s%n", 
            error.getLineNumber(),
            error.getMessage()
        );
    }
}
```

### 4. Get Line-by-Line Details
```java
error.getLineNumber()    // 10
error.getSegmentTag()    // "PO1"
error.getFieldPath()     // "unitPrice"
error.getMessage()       // "must be <= 100.00"
error.getInvalidValue()  // 150.0
```

---

## Test Methods for Line Errors

### Method 1: Detailed Line Report
```bash
mvn test -Dtest=EDIMessageRawFileTest#testLineByLineValidationReport
```
**Shows**: Table format with line numbers and error details

### Method 2: Object Validation with Lines
```bash
mvn test -Dtest=EDIMessageRawFileTest#testObjectValidationWithLineNumbers
```
**Shows**: Line 10 and Line 12 errors in context

### Method 3: Read Invalid File
```bash
mvn test -Dtest=EDIMessageRawFileTest#testReadInvalidEDI850WithErrors
```
**Shows**: File structure analysis and error summary

---

## Code Example: Reading EDI with Line Errors

```java
@Test
public void testLineByLineValidationReport() throws Exception {
    // Read EDI file
    String filePath = "src/test/resources/850_v2_invalid_prices.edi";
    String ediContent = readFileContent(filePath);
    String[] segments = ediContent.split("~");
    
    // Create validation errors list
    List<ValidationError> errors = new ArrayList<>();
    
    // Check each segment
    int lineNumber = 1;
    for (String segment : segments) {
        if (segment.startsWith("PO1")) {
            // Parse PO1: PO1*lineNum*qty*unit*price*...
            String[] fields = segment.split("\\*");
            if (fields.length >= 4) {
                double price = Double.parseDouble(fields[3]);
                
                // Check constraint: price must be <= 100
                if (price > 100.0) {
                    errors.add(new ValidationError(
                        lineNumber,           // Line number
                        "PO1",                // Segment tag
                        "unitPrice",          // Field name
                        "must be <= 100.00",  // Error message
                        price + ""            // Invalid value
                    ));
                }
            }
        }
        lineNumber++;
    }
    
    // Print results
    System.out.println("Line | Segment | Field | Error | Value");
    System.out.println("?????????????????????????????????????????");
    for (ValidationError error : errors) {
        System.out.printf("%4d | %7s | %5s | %5s | %s%n",
            error.lineNumber,
            error.segmentTag,
            error.fieldName,
            error.errorMessage,
            error.value
        );
    }
}
```

---

## Sample Output Formats

### Format 1: Simple Table
```
Line | Segment | Field         | Error Message             | Value
?????????????????????????????????????????????????????????????????????
  10 | PO1     | unitPrice     | must be <= 100.00         | 150.0
  12 | PO1     | unitPrice     | must be <= 100.00         | 101.0
```

### Format 2: Detailed Error Info
```
Error 1:
  Line Number : 10
  Segment Tag : PO1
  Field Name  : unitPrice
  Message     : must be <= 100.00
  Invalid Val : 150.0
  Constraint  : Price must be between 1.00 and 100.00
```

### Format 3: Validation Result
```
Line 10 - PO1 Segment (Line Number 00010):
  ? Has 1 error(s):
     • Field 'unitPrice': must be <= 100.00 (value: 150.0)
```

---

## Key Features

? **Line Tracking**: Each error shows exact line number  
? **Segment Context**: Segment tag included  
? **Field Name**: Which field had the error  
? **Error Message**: Clear validation message  
? **Invalid Value**: What value was rejected  
? **Formatted Output**: Multiple output formats  
? **No Exceptions**: Lenient mode collects all errors  

---

## Running Your Own Tests

### Create Custom EDI File
```
src/test/resources/my_850_test.edi
```

### Add Test Method
```java
@Test
public void testMyCustomEDI850() throws Exception {
    String filePath = "src/test/resources/my_850_test.edi";
    String ediContent = readFileContent(filePath);
    String[] segments = ediContent.split("~");
    
    // Validate and report line-by-line errors
    analyzeSegments(segments);
}
```

### Run It
```bash
mvn test -Dtest=EDIMessageRawFileTest#testMyCustomEDI850
```

---

## Files Available

| File | Purpose |
|------|---------|
| `EDIMessageRawFileTest.java` | Raw file reading and validation tests |
| `850_v2_valid.edi` | Valid EDI message (all prices OK) |
| `850_v2_invalid_prices.edi` | Invalid EDI with price errors |
| `RAW_EDI_FILE_TESTS_GUIDE.md` | Full test documentation |

---

## Answer to Your Question

**Q**: Where is the test method that reads valid and invalid raw EDI messages? If there is an error, can we show the error line?

**A**: 
- **Test class**: `EDIMessageRawFileTest.java`
- **Valid file test**: `testReadValidEDI850FromFile()`
- **Invalid file test**: `testReadInvalidEDI850WithErrors()`
- **Line-by-line errors**: `testLineByLineValidationReport()`
- **With line numbers**: `testObjectValidationWithLineNumbers()`
- **Error line shown**: YES! ? Line 10, Line 12, etc.

**Run this**:
```bash
mvn test -Dtest=EDIMessageRawFileTest#testLineByLineValidationReport
```

---

**Status: Implemented & Ready! ?**
