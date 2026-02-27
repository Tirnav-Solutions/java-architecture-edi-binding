# Quick Test Commands

## Run All Tests

### Run all 4 EDIUnmarshaller tests
```bash
mvn test -Dtest=EDIMessageRawFileTest
```

### Run specific test
```bash
# Test 1: Valid EDI unmarshalling
mvn test -Dtest=EDIMessageRawFileTest#testUnmarshalValidEDI850FromFile

# Test 2: Invalid EDI with validation errors
mvn test -Dtest=EDIMessageRawFileTest#testUnmarshalInvalidEDI850WithErrors

# Test 3: Lenient validation showing line-by-line errors
mvn test -Dtest=EDIMessageRawFileTest#testValidateWithLenientModeShowLineErrors

# Test 4: Strict vs Lenient mode comparison
mvn test -Dtest=EDIMessageRawFileTest#testStrictVsLenientValidation
```

---

## What Each Test Does

### Test 1: testUnmarshalValidEDI850FromFile()
- **File**: `850_v2_valid.edi` (prices: 50.00, 75.00)
- **Method**: `EDIUnmarshaller.unmarshal(TestEDI850.class, fileReader)`
- **Result**: ? Successfully unmarshalls and parses
- **Shows**: Parsed items with valid prices

### Test 2: testUnmarshalInvalidEDI850WithErrors()
- **File**: `850_v2_invalid_prices.edi` (prices: 150.00, 101.00)
- **Method**: `EDIUnmarshaller.unmarshal(TestEDI850.class, fileReader)`
- **Result**: ? Throws EDIMessageException (validation fails)
- **Shows**: Which prices violate constraints

### Test 3: testValidateWithLenientModeShowLineErrors()
- **Method**: `FieldAwareConverter.validateObjectLenient(item, "PO1", lineNumber)`
- **Mode**: LENIENT (collects errors, no exception)
- **Result**: ? 2 validation errors
- **Shows**: Line 10 error, Line 12 error in table format

### Test 4: testStrictVsLenientValidation()
- **Strict**: `FieldAwareConverter.validateObject(item)` ? throws
- **Lenient**: `FieldAwareConverter.validateObjectLenient(item, "PO1", 10)` ? collects
- **Shows**: Difference between two modes

---

## Key Output Patterns

### Valid EDI Output
```
? SUCCESS: EDI 850 unmarshalled successfully!
   Items parsed: 2
   PO1 Items:
     Line 1: PO1 - Qty=5, Price=50.00 ? (within 1-100)
     Line 2: PO1 - Qty=5, Price=75.00 ? (within 1-100)
```

### Invalid EDI Output
```
? VALIDATION ERROR CAUGHT (EXPECTED):

Validation failed for TestPO1Segment:
  - unitPrice: must be less than or equal to 100 (invalid value: 150.0)

? ANALYSIS:
   - Line 10 (PO1): Price 150.00 exceeds maximum 100.00
   - Line 12 (PO1): Price 101.00 exceeds maximum 100.00
```

### Lenient Mode Output
```
Line | Segment | Field     | Constraint      | Invalid Value
??????????????????????????????????????????????????????????????
  10 | PO1     | unitPrice | max=100         | 150.0
  12 | PO1     | unitPrice | max=100         | 101.0
```

---

## EDI Files Used

- **Valid**: `src/test/resources/850_v2_valid.edi`
- **Invalid**: `src/test/resources/850_v2_invalid_prices.edi`

---

## Compilation

```bash
mvn clean compile -DskipTests
```

**Status**: ? BUILD SUCCESS

---

## Key Features

? Uses `EDIUnmarshaller.unmarshal()` to parse real EDI files  
? Shows validation errors with line numbers (Line 10, Line 12, etc.)  
? Demonstrates STRICT validation (throws exception)  
? Demonstrates LENIENT validation (collects errors)  
? Price validation: @Min(1) @Max(100)  
? Line-by-line error reporting  
? Formatted error output (table format)  

---

## Files Modified

- **EDIMessageRawFileTest.java** - Updated with EDIUnmarshaller calls
- **850_v2_valid.edi** - Valid EDI file for testing
- **850_v2_invalid_prices.edi** - Invalid EDI file for testing

---

**Ready to run!** ?
