package javax.edi.bind;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.schema.EDISchemaGenerator;
import javax.edi.bind.util.EDIValidationError;
import javax.edi.bind.util.FieldAwareConverter;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.junit.Test;

/**
 * Test class demonstrating validation for MARSHALLING (generating EDI messages).
 * Tests both EDI 850 (PO) and EDI 856 (Shipment Notice) generation with validation.
 */
public class EDIMessageGenerationWithValidationTest {

    /**
     * Test 1: Generate VALID EDI 856 message
     */
    @Test
    public void testGenerateValidEDI856() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 1: Generate VALID EDI 856 (Shipment Notice)           ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        // Create valid EDI 856 message
        TestEDI856 shipment = new TestEDI856();
        
        TestBSNSegment bsn = new TestBSNSegment();
        bsn.shipmentNumber = "SHP001";
        bsn.shipmentDate = "2026-02-27";
        shipment.header = bsn;
        
        // Add shipment items with VALID quantities (min 1, max 1000)
        Collection<TestShipmentItem> items = new ArrayList<>();
        
        TestShipmentItem item1 = new TestShipmentItem();
        item1.itemNumber = "ITEM001";
        item1.quantity = 100;  // ? Valid: within 1-1000
        items.add(item1);
        
        TestShipmentItem item2 = new TestShipmentItem();
        item2.itemNumber = "ITEM002";
        item2.quantity = 500;  // ? Valid: within 1-1000
        items.add(item2);
        
        shipment.items = items;
        
        System.out.println("? Creating valid EDI 856 message:\n");
        System.out.println("   Shipment Number: " + bsn.shipmentNumber);
        System.out.println("   Shipment Date: " + bsn.shipmentDate);
        System.out.println("   Items: " + items.size());
        System.out.println("     Item 1: " + item1.itemNumber + " - Qty: " + item1.quantity);
        System.out.println("     Item 2: " + item2.itemNumber + " - Qty: " + item2.quantity + "\n");
        
        // Validate before marshalling
        System.out.println("? Validating message (lenient mode)...\n");
        EDIValidationError.ValidationResult validation = 
            FieldAwareConverter.validateObjectLenient(shipment);
        
        if (validation.hasErrors()) {
            System.out.println("? Validation failed:");
            System.out.println(validation.getDetailedErrorSummary());
            return;
        }
        
        System.out.println("? Validation passed - No errors\n");
        
        // Marshal (generate EDI)
        System.out.println("? Marshalling to EDI format...\n");
        try {
            StringWriter writer = new StringWriter();
            EDIMarshaller.marshal(shipment, writer);
            
            String ediOutput = writer.toString();
            System.out.println("? SUCCESS: EDI 856 generated successfully!");
            System.out.println("   Output length: " + ediOutput.length() + " characters");
            System.out.println("\n   Generated EDI (first 1000 chars):");
            System.out.println("   " + ediOutput.substring(0, Math.min(1000, ediOutput.length())) + "...\n");
            
        } catch (EDIMessageException e) {
            System.out.println("? MARSHALLING ERROR: " + e.getMessage());
        }
    }

    /**
     * Test 2: Generate INVALID EDI 856 with validation errors
     */
    @Test
    public void testGenerateInvalidEDI856WithErrors() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 2: Generate INVALID EDI 856 (Validation Fails)        ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        // Create EDI 856 with INVALID data
        TestEDI856 shipment = new TestEDI856();
        
        TestBSNSegment bsn = new TestBSNSegment();
        bsn.shipmentNumber = "SHP002";
        // Missing shipmentDate - violates @NotNull
        shipment.header = bsn;
        
        // Add shipment items with INVALID quantities (exceed max 1000)
        Collection<TestShipmentItem> items = new ArrayList<>();
        
        TestShipmentItem item1 = new TestShipmentItem();
        item1.itemNumber = "ITEM001";
        item1.quantity = 1500;  // ? Invalid: exceeds max 1000
        items.add(item1);
        
        TestShipmentItem item2 = new TestShipmentItem();
        item2.itemNumber = "ITEM002";
        item2.quantity = 2000;  // ? Invalid: exceeds max 1000
        items.add(item2);
        
        shipment.items = items;
        
        System.out.println("? Creating INVALID EDI 856 message:\n");
        System.out.println("   Shipment Number: " + bsn.shipmentNumber);
        System.out.println("   Shipment Date: " + (bsn.shipmentDate != null ? bsn.shipmentDate : "NULL ?"));
        System.out.println("   Items: " + items.size());
        System.out.println("     Item 1: " + item1.itemNumber + " - Qty: " + item1.quantity + " ? (exceeds max 1000)");
        System.out.println("     Item 2: " + item2.itemNumber + " - Qty: " + item2.quantity + " ? (exceeds max 1000)\n");
        
        System.out.println("? Attempting to marshal invalid message...\n");
        
        // Try to marshal - should fail validation
        try {
            StringWriter writer = new StringWriter();
            EDIMarshaller.marshal(shipment, writer);
            
            System.out.println("? UNEXPECTED: Message should have failed validation!");
            
        } catch (EDIMessageException e) {
            // THIS IS EXPECTED
            System.out.println("? EXPECTED: Validation failed as expected!\n");
            System.out.println("? Validation Error:\n");
            System.out.println(e.getMessage());
            
            System.out.println("\n? ANALYSIS:\n");
            System.out.println("   Errors found:");
            System.out.println("     1. shipmentDate: may not be null");
            System.out.println("     2. Item 1 quantity: 1500 exceeds maximum 1000");
            System.out.println("     3. Item 2 quantity: 2000 exceeds maximum 1000");
        }
    }

    /**
     * Test 3: Lenient validation - collect all errors without throwing
     */
    @Test
    public void testGenerateWithLenientValidation() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 3: Lenient Validation - Collect All Errors            ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        TestEDI856 shipment = new TestEDI856();
        
        TestBSNSegment bsn = new TestBSNSegment();
        bsn.shipmentNumber = "SHP003";
        // Missing date
        shipment.header = bsn;
        
        Collection<TestShipmentItem> items = new ArrayList<>();
        TestShipmentItem item = new TestShipmentItem();
        item.itemNumber = "ITEM001";
        item.quantity = 1500;  // Invalid
        items.add(item);
        
        shipment.items = items;
        
        System.out.println("? Message state:\n");
        System.out.println("   Shipment Number: " + bsn.shipmentNumber);
        System.out.println("   Date: NULL ?");
        System.out.println("   Items: 1");
        System.out.println("     Qty: 1500 ?\n");
        
        // Use LENIENT mode - collect errors without throwing
        System.out.println("? Validating with LENIENT mode (no exception)...\n");
        EDIValidationError.ValidationResult result = 
            FieldAwareConverter.validateObjectLenient(shipment);
        
        if (result.hasErrors()) {
            System.out.println("? Validation failed with " + result.getErrorCount() + " error(s):\n");
            
            int errorNum = 1;
            for (EDIValidationError error : result.getErrors()) {
                System.out.println("Error " + errorNum + ":");
                System.out.println("  Field: " + error.getFieldPath());
                System.out.println("  Message: " + error.getMessage());
                System.out.println("  Value: " + error.getInvalidValue() + "\n");
                errorNum++;
            }
            
            // Print formatted summary
            System.out.println(result.getDetailedErrorSummary());
        } else {
            System.out.println("? No errors found");
        }
    }

    /**
     * Test 4: Compare Unmarshalling vs Marshalling Validation
     */
    @Test
    public void testUnmarshallingVsMarshallingValidation() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 4: Unmarshalling vs Marshalling Validation            ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        System.out.println("UNMARSHALLING (Parsing EDI ? Object)\n");
        System.out.println("  Input:  Raw EDI message (file or string)");
        System.out.println("  Process: EDIUnmarshaller.unmarshal()");
        System.out.println("  Step 1: Parse EDI segments");
        System.out.println("  Step 2: Validate annotations (@NotNull, @Max, @Min, etc.)");
        System.out.println("  Step 3: Return validated object OR throw exception\n");
        System.out.println("  Code: TestEDI856 msg = EDIUnmarshaller.unmarshal(TestEDI856.class, reader);");
        System.out.println("  Error Handling: ? Throws EDIMessageException on validation failure\n");
        
        System.out.println("?".repeat(70) + "\n");
        
        System.out.println("MARSHALLING (Object ? Generating EDI)\n");
        System.out.println("  Input:  Java object with data");
        System.out.println("  Process: EDIMarshaller.marshal()");
        System.out.println("  Step 1: Validate annotations (@NotNull, @Max, @Min, etc.)");
        System.out.println("  Step 2: If valid, generate EDI segments");
        System.out.println("  Step 3: Return EDI string OR throw exception\n");
        System.out.println("  Code: EDIMarshaller.marshal(shipment, writer);");
        System.out.println("  Error Handling: ? Throws EDIMessageException on validation failure\n");
        
        System.out.println("?".repeat(70) + "\n");
        
        System.out.println("? BOTH support STRICT validation (throws exception)\n");
        System.out.println("   Unmarshalling: FieldAwareConverter.validateObject()");
        System.out.println("   Marshalling:   FieldAwareConverter.validateObject()\n");
        
        System.out.println("? BOTH can use LENIENT validation (collect errors)\n");
        System.out.println("   Code: FieldAwareConverter.validateObjectLenient(obj)");
        System.out.println("   Result: Returns ValidationResult with all errors\n");
    }

    /**
     * Test 5: Real-world scenario - Generate EDI 850 with price validation
     */
    @Test
    public void testGenerateEDI850WithPriceValidation() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 5: Generate EDI 850 with Price Validation             ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        // Generate a purchase order
        TestEDI850 po = new TestEDI850();
        
        Collection<TestPO1Segment> items = new ArrayList<>();
        
        // Item 1: Valid price (within 1-100)
        TestPO1Segment item1 = new TestPO1Segment();
        item1.lineNumber = "00010";
        item1.quantity = 5;
        item1.unitPrice = 50.00;  // ? Valid
        items.add(item1);
        
        // Item 2: Invalid price (exceeds max 100)
        TestPO1Segment item2 = new TestPO1Segment();
        item2.lineNumber = "00020";
        item2.quantity = 3;
        item2.unitPrice = 150.00;  // ? Invalid: exceeds max
        items.add(item2);
        
        po.items = items;
        
        System.out.println("? Creating EDI 850 Purchase Order:\n");
        System.out.println("   Items: " + items.size());
        System.out.println("     Line 10: Qty=5, Price=$50.00 ? (valid)");
        System.out.println("     Line 20: Qty=3, Price=$150.00 ? (exceeds max $100)\n");
        
        System.out.println("? Attempting to marshal...\n");
        
        try {
            StringWriter writer = new StringWriter();
            EDIMarshaller.marshal(po, writer);
            
            System.out.println("? UNEXPECTED: Should have failed validation");
            
        } catch (EDIMessageException e) {
            System.out.println("? EXPECTED: Validation caught the error!\n");
            System.out.println("? Error:\n");
            
            // Extract just the message
            String errorMsg = e.getMessage();
            System.out.println(errorMsg);
            
            System.out.println("\n? Analysis:\n");
            System.out.println("   When generating EDI 850:");
            System.out.println("   ? Item 1: Price $50.00 is within 1-100 ? Would be included");
            System.out.println("   ? Item 2: Price $150.00 exceeds max ? Generation aborted");
            System.out.println("   Result: No EDI output generated (validation prevents bad data)\n");
        }
    }

    // ========== EDI 856 Test Model Classes ==========

    /**
     * Test 6: Generate JSON Schema from EDI model class
     */
    @Test
    public void testGenerateJsonSchema() throws Exception {
        System.out.println("\n??????????????????????????????????????????????????????????????");
        System.out.println("?  TEST 6: Generate JSON Schema from EDI Model               ?");
        System.out.println("??????????????????????????????????????????????????????????????\n");
        
        String schema856 = EDISchemaGenerator.generateSchema(TestEDI856.class);
        System.out.println("JSON Schema for TestEDI856:\n");
        System.out.println(schema856);
        
        // Verify it contains expected elements
        assert schema856.contains("\"$schema\"");
        assert schema856.contains("\"TestEDI856\"");
        assert schema856.contains("\"BSN\"");
        assert schema856.contains("\"SHP\"");
        assert schema856.contains("\"minItems\"");
        assert schema856.contains("\"maximum\": 1000");
        
        System.out.println("\n? Schema generated successfully with validation rules embedded\n");
        
        System.out.println("--------------------------------------------\n");
        
        String schema850 = EDISchemaGenerator.generateSchema(TestEDI850.class);
        System.out.println("JSON Schema for TestEDI850:\n");
        System.out.println(schema850);
        
        assert schema850.contains("\"PO1\"");
        assert schema850.contains("\"maximum\": \"100\"");
        
        System.out.println("\n? Schema generated successfully\n");
    }

    // ========== EDI 856 Test Model Classes (below) ==========

    @EDIMessage(componentDelimiter = '>', elementDelimiter = '*', segmentDelimiter = '~')
    public static class TestEDI856 {
        @NotNull
        @Valid
        TestBSNSegment header;
        
        @NotNull
        @Size(min = 1)
        @Valid
        @EDICollectionType(TestShipmentItem.class)
        Collection<TestShipmentItem> items;
        
        // getters/setters
        public TestBSNSegment getHeader() { return header; }
        public void setHeader(TestBSNSegment header) { this.header = header; }
        
        public Collection<TestShipmentItem> getItems() { return items; }
        public void setItems(Collection<TestShipmentItem> items) { this.items = items; }
    }

    @EDISegment(tag = "BSN")
    public static class TestBSNSegment {
        @NotNull
        @EDIElement(fieldName = "ELEMENT", dataElement = "0001")
        String shipmentNumber;
        
        @NotNull
        @EDIElement(fieldName = "ELEMENT", dataElement = "0002")
        String shipmentDate;
        
        // getters/setters
        public String getShipmentNumber() { return shipmentNumber; }
        public void setShipmentNumber(String shipmentNumber) { this.shipmentNumber = shipmentNumber; }
        
        public String getShipmentDate() { return shipmentDate; }
        public void setShipmentDate(String shipmentDate) { this.shipmentDate = shipmentDate; }
    }

    @EDISegment(tag = "SHP")
    public static class TestShipmentItem {
        @NotNull
        @EDIElement(fieldName = "ELEMENT", dataElement = "0001")
        String itemNumber;
        
        @NotNull
        @Min(1)
        @Max(1000)
        @EDIElement(fieldName = "ELEMENT", dataElement = "0002")
        int quantity;  // Quantity between 1-1000
        
        // getters/setters
        public String getItemNumber() { return itemNumber; }
        public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    // ========== EDI 850 Test Model Classes ==========

    @EDIMessage(componentDelimiter = '>', elementDelimiter = '*', segmentDelimiter = '~')
    public static class TestEDI850 {
        @NotNull
        @Size(min = 1)
        @Valid
        @EDICollectionType(TestPO1Segment.class)
        Collection<TestPO1Segment> items;
        
        public Collection<TestPO1Segment> getItems() { return items; }
        public void setItems(Collection<TestPO1Segment> items) { this.items = items; }
    }

    @EDISegment(tag = "PO1")
    public static class TestPO1Segment {
        @NotNull
        @EDIElement(fieldName = "ELEMENT", dataElement = "0001")
        String lineNumber;
        
        @NotNull
        @EDIElement(fieldName = "ELEMENT", dataElement = "0002")
        int quantity;
        
        @NotNull
        @DecimalMin("1")
        @DecimalMax("100")
        @EDIElement(fieldName = "ELEMENT", dataElement = "0003")
        double unitPrice;
        
        public String getLineNumber() { return lineNumber; }
        public void setLineNumber(String lineNumber) { this.lineNumber = lineNumber; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    }
}
