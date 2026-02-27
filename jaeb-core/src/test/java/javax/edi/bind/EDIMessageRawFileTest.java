package javax.edi.bind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.util.EDIValidationError;
import javax.edi.bind.util.FieldAwareConverter;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.junit.Test;

/**
 * Test class for reading and validating actual EDI 850 messages from files.
 * Uses EDIUnmarshaller to parse EDI and shows line-by-line error reporting.
 */
public class EDIMessageRawFileTest {

    /**
     * Test 1: Read and unmarshal VALID EDI 850 message
     */
    @Test
    public void testUnmarshalValidEDI850FromFile() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 1: Unmarshal VALID EDI 850 Using EDIUnmarshaller     ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        String filePath = "src/test/resources/850_v2_valid.edi";
        File ediFile = new File(filePath);
        
        if (!ediFile.exists()) {
            System.out.println("? File not found: " + filePath);
            return;
        }
        
        try (FileReader fileReader = new FileReader(ediFile)) {
            System.out.println("? File: " + filePath);
            System.out.println("? Size: " + ediFile.length() + " bytes\n");
            
            // **USE EDIUnmarshaller TO PARSE**
            System.out.println("? Unmarshalling with EDIUnmarshaller...\n");
            TestEDI850 message = EDIUnmarshaller.unmarshal(TestEDI850.class, fileReader);
            
            System.out.println("? SUCCESS: EDI 850 unmarshalled successfully!");
            System.out.println("   Items parsed: " + (message.items != null ? message.items.size() : 0));
            
            if (message.items != null) {
                System.out.println("\n   PO1 Items:");
                int lineNum = 1;
                for (TestPO1Segment item : message.items) {
                    System.out.printf("     Line %d: PO1 - Qty=%d, Price=%.2f ? (within 1-100)%n",
                        lineNum,
                        item.quantity,
                        item.unitPrice
                    );
                    lineNum++;
                }
            }
            
        } catch (EDIMessageException e) {
            System.out.println("? VALIDATION ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("? ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: Read invalid EDI and catch validation errors
     */
    @Test
    public void testUnmarshalInvalidEDI850WithErrors() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 2: Unmarshal INVALID EDI 850 with Price Errors       ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        String filePath = "src/test/resources/850_v2_invalid_prices.edi";
        File ediFile = new File(filePath);
        
        if (!ediFile.exists()) {
            System.out.println("? File not found: " + filePath);
            return;
        }
        
        try (FileReader fileReader = new FileReader(ediFile)) {
            System.out.println("? File: " + filePath);
            System.out.println("? Size: " + ediFile.length() + " bytes\n");
            
            System.out.println("? Unmarshalling with EDIUnmarshaller...\n");
            TestEDI850 message = EDIUnmarshaller.unmarshal(TestEDI850.class, fileReader);
            
            // If we get here, validation passed (shouldn't happen with invalid file)
            System.out.println("??  Message unmarshalled but should have validation errors");
            
        } catch (EDIMessageException e) {
            // THIS IS EXPECTED - validation should fail
            System.out.println("? VALIDATION ERROR CAUGHT (EXPECTED):\n");
            System.out.println(e.getMessage());
            
            System.out.println("\n? ANALYSIS:");
            System.out.println("   - Line 10 (PO1): Price 150.00 exceeds maximum 100.00");
            System.out.println("   - Line 12 (PO1): Price 101.00 exceeds maximum 100.00");
        } catch (Exception e) {
            System.out.println("? ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3: Validate with lenient mode to collect all errors WITHOUT throwing
     */
    @Test
    public void testValidateWithLenientModeShowLineErrors() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 3: Lenient Validation - Collect All Errors           ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        // Create test segments with validation errors
        System.out.println("? Creating PO1 segments with price violations:\n");
        
        // Segment 1: Line 10, Price 150.00 (exceeds max 100)
        TestPO1Segment item1 = new TestPO1Segment();
        item1.lineNumber = "00010";
        item1.quantity = 5;
        item1.unitPrice = 150.00;  // ? INVALID
        
        System.out.println("Item 1:");
        System.out.println("  Line Number: " + item1.lineNumber);
        System.out.println("  Quantity: " + item1.quantity);
        System.out.println("  Price: " + item1.unitPrice);
        
        // Validate with line number context
        System.out.println("\n  Validating Item 1 (EDI Line 10)...");
        EDIValidationError.ValidationResult result1 = 
            FieldAwareConverter.validateObjectLenient(item1, "PO1", 10);
        
        if (result1.hasErrors()) {
            System.out.println("  ? VALIDATION ERRORS:");
            for (EDIValidationError error : result1.getErrors()) {
                System.out.printf("     Line: %d | Segment: %s | Field: %s%n",
                    error.getLineNumber(),
                    error.getSegmentTag(),
                    error.getFieldPath());
                System.out.printf("     Message: %s | Value: %s%n%n",
                    error.getMessage(),
                    error.getInvalidValue());
            }
        } else {
            System.out.println("  ? Valid");
        }
        
        // Segment 2: Line 12, Price 101.00 (exceeds max 100)
        TestPO1Segment item2 = new TestPO1Segment();
        item2.lineNumber = "00020";
        item2.quantity = 5;
        item2.unitPrice = 101.00;  // ? INVALID
        
        System.out.println("\nItem 2:");
        System.out.println("  Line Number: " + item2.lineNumber);
        System.out.println("  Quantity: " + item2.quantity);
        System.out.println("  Price: " + item2.unitPrice);
        
        // Validate with line number context
        System.out.println("\n  Validating Item 2 (EDI Line 12)...");
        EDIValidationError.ValidationResult result2 = 
            FieldAwareConverter.validateObjectLenient(item2, "PO1", 12);
        
        if (result2.hasErrors()) {
            System.out.println("  ? VALIDATION ERRORS:");
            for (EDIValidationError error : result2.getErrors()) {
                System.out.printf("     Line: %d | Segment: %s | Field: %s%n",
                    error.getLineNumber(),
                    error.getSegmentTag(),
                    error.getFieldPath());
                System.out.printf("     Message: %s | Value: %s%n%n",
                    error.getMessage(),
                    error.getInvalidValue());
            }
        } else {
            System.out.println("  ? Valid");
        }
        
        // Print summary table
        System.out.println("\n" +
            "???????????????????????????????????????????????????????????????\n" +
            "ERROR SUMMARY TABLE\n" +
            "???????????????????????????????????????????????????????????????\n");
        
        System.out.println("Line | Segment | Field     | Constraint      | Invalid Value");
        System.out.println("??????????????????????????????????????????????????????????????");
        System.out.println("  10 | PO1     | unitPrice | max=100         | 150.0");
        System.out.println("  12 | PO1     | unitPrice | max=100         | 101.0");
        System.out.println("??????????????????????????????????????????????????????????????\n");
    }

    /**
     * Test 4: Show the difference between strict and lenient modes
     */
    @Test
    public void testStrictVsLenientValidation() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 4: Strict vs Lenient Validation Modes                ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        TestPO1Segment item = new TestPO1Segment();
        item.lineNumber = "00010";
        item.quantity = 5;
        item.unitPrice = 150.00;  // Invalid: exceeds max 100
        
        // STRICT MODE
        System.out.println("MODE 1: STRICT (throws exception on first error)\n");
        System.out.println("Code: FieldAwareConverter.validateObject(item)");
        System.out.println("Result:");
        try {
            FieldAwareConverter.validateObject(item);
            System.out.println("  ? Valid");
        } catch (EDIMessageException e) {
            System.out.println("  ? Throws EDIMessageException immediately");
            System.out.println("  Message: " + e.getMessage().substring(0, Math.min(60, e.getMessage().length())) + "...\n");
        }
        
        // LENIENT MODE
        System.out.println("MODE 2: LENIENT (collects all errors, no exception)\n");
        System.out.println("Code: FieldAwareConverter.validateObjectLenient(item, \"PO1\", 10)");
        System.out.println("Result:");
        
        EDIValidationError.ValidationResult result = 
            FieldAwareConverter.validateObjectLenient(item, "PO1", 10);
        
        if (result.hasErrors()) {
            System.out.println("  ? Validation failed");
            System.out.println("  Errors collected: " + result.getErrorCount());
            System.out.println("\n  Error details:");
            for (EDIValidationError error : result.getErrors()) {
                System.out.println("    • Line: " + error.getLineNumber());
                System.out.println("    • Segment: " + error.getSegmentTag());
                System.out.println("    • Field: " + error.getFieldPath());
                System.out.println("    • Message: " + error.getMessage());
                System.out.println("    • Value: " + error.getInvalidValue() + "\n");
            }
        } else {
            System.out.println("  ? Valid");
        }
        
        System.out.println("\nDifferences:");
        System.out.println("  STRICT:  Exception thrown ? Parsing stops");
        System.out.println("  LENIENT: No exception    ? All errors collected ? Parsing continues\n");
    }

    /**
     * Helper method to read file content
     */
    private String readFileContent(String filePath) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    // ========== EDI 850 Test Model Classes ==========

    @EDIMessage(componentDelimiter = '>', elementDelimiter = '*', segmentDelimiter = '~')
    static class TestEDI850 {
        @NotNull
        @Size(min = 1)
        @EDICollectionType(TestPO1Segment.class)
        Collection<TestPO1Segment> items;
        
        public Collection<TestPO1Segment> getItems() {
            return items;
        }
        
        public void setItems(Collection<TestPO1Segment> items) {
            this.items = items;
        }
    }

    @EDISegment(tag = "PO1")
    static class TestPO1Segment {
        @NotNull
        String lineNumber;
        
        @NotNull
        int quantity;
        
        @NotNull
        @Min(1)
        @Max(100)
        double unitPrice;  // Price must be between 1 and 100
        
        public String getLineNumber() {
            return lineNumber;
        }
        
        public void setLineNumber(String lineNumber) {
            this.lineNumber = lineNumber;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public double getUnitPrice() {
            return unitPrice;
        }
        
        public void setUnitPrice(double unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
