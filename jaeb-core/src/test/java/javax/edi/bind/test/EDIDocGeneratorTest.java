package javax.edi.bind.test;

import static org.junit.Assert.*;

import javax.edi.bind.schema.EDIDocGenerator;
import javax.edi.bind.test.beans.ExampleMessage;

import org.junit.Test;

/**
 * Tests for {@link EDIDocGenerator}.
 */
public class EDIDocGeneratorTest {

    @Test
    public void testGenerateText_notNull() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        System.out.println(doc); // Print the generated document for visual inspection
        assertNotNull("Generated document should not be null", doc);
        assertFalse("Generated document should not be empty", doc.isEmpty());
    }

    @Test
    public void testGenerateText_containsHeader() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain guide header", doc.contains("EDI IMPLEMENTATION GUIDE"));
        assertTrue("Should contain class name", doc.contains("ExampleMessage"));
    }

    @Test
    public void testGenerateText_containsDelimiters() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain element delimiter", doc.contains("Element Delimiter"));
        assertTrue("Should contain segment delimiter", doc.contains("Segment Delimiter"));
        assertTrue("Should contain component delimiter", doc.contains("Component Delimiter"));
    }

    @Test
    public void testGenerateText_containsSegmentTag() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain ITEM segment tag", doc.contains("ITEM"));
    }

    @Test
    public void testGenerateText_containsSegmentGroup() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain Group reference", doc.contains("Group"));
        assertTrue("Should contain PRICING header", doc.contains("PRICING"));
    }

    @Test
    public void testGenerateText_containsLoop() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain LOOP for collection segments", doc.contains("LOOP"));
    }

    @Test
    public void testGenerateText_containsTableOfContents() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain TABLE OF CONTENTS", doc.contains("TABLE OF CONTENTS"));
    }

    @Test
    public void testGenerateText_containsSummary() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain SUMMARY section", doc.contains("SUMMARY"));
        assertTrue("Should contain Total Segments count", doc.contains("Total Segments"));
        assertTrue("Should contain Total Elements count", doc.contains("Total Elements"));
        assertTrue("Should contain Required count", doc.contains("Required"));
        assertTrue("Should contain Optional count", doc.contains("Optional"));
    }

    @Test
    public void testGenerateText_containsComposite() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain COMPOSITE for component elements", doc.contains("COMPOSITE"));
    }

    @Test
    public void testGenerateText_containsSegmentFooter() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain footer reference", doc.contains("ENDPRICING"));
    }

    @Test
    public void testGenerateText_containsFormatConstraints() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain date format", doc.contains("fmt=yyyyMMdd"));
    }

    @Test
    public void testGenerateText_containsEndMarker() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain end marker", doc.contains("END OF IMPLEMENTATION GUIDE"));
    }

    @Test
    public void testGenerateText_containsPriceSegment() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class);
        assertTrue("Should contain PRICE segment tag", doc.contains("PRICE"));
    }

    // ========== Markdown Format Tests ==========

    @Test
    public void testGenerateMarkdown_notNull() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertNotNull("Generated markdown should not be null", doc);
        assertFalse("Generated markdown should not be empty", doc.isEmpty());
    }

    @Test
    public void testGenerateMarkdown_containsHeader() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain markdown h1 header", doc.contains("# EDI Implementation Guide"));
    }

    @Test
    public void testGenerateMarkdown_containsDelimiterTable() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain Property table", doc.contains("| Property | Value |"));
        assertTrue("Should contain element delimiter row", doc.contains("Element Delimiter"));
    }

    @Test
    public void testGenerateMarkdown_containsTableOfContents() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain Table of Contents heading", doc.contains("## Table of Contents"));
    }

    @Test
    public void testGenerateMarkdown_containsStructure() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain Structure heading", doc.contains("## Structure"));
    }

    @Test
    public void testGenerateMarkdown_containsSummary() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain Summary heading", doc.contains("## Summary"));
        assertTrue("Should contain metric table", doc.contains("| Metric | Count |"));
    }

    @Test
    public void testGenerateMarkdown_containsSegmentTable() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain element table header", doc.contains("| Pos | Element | Field | Type | Required |"));
    }

    @Test
    public void testGenerateMarkdown_containsComposite() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain COMPOSITE in markdown", doc.contains("COMPOSITE"));
    }

    @Test
    public void testGenerateMarkdown_containsSegmentTags() {
        String doc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.MARKDOWN);
        assertTrue("Should contain ITEM segment", doc.contains("ITEM"));
        assertTrue("Should contain PRICE segment", doc.contains("PRICE"));
    }

    // ========== Default Format Test ==========

    @Test
    public void testGenerateDefaultFormat_isText() {
        String textDoc = EDIDocGenerator.generate(ExampleMessage.class);
        String explicitTextDoc = EDIDocGenerator.generate(ExampleMessage.class, EDIDocGenerator.Format.TEXT);
        assertEquals("Default format should be TEXT", explicitTextDoc, textDoc);
    }
}
