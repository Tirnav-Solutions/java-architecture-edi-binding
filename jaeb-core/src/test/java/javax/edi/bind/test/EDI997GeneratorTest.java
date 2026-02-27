package javax.edi.bind.test;

import static org.junit.Assert.*;

import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;

import org.junit.Test;

/**
 * Tests for {@link EDI997Generator}.
 * Tests the public API only.
 */
public class EDI997GeneratorTest {

    @Test
    public void testGenerateFromNullResult() {
        EDI997Generator.Result result = EDI997Generator.generate((EDIUnmarshalResult<?>) null);
        assertFalse("Should not succeed with null result", result.isSuccess());
        assertNotNull("Should have error message", result.getError());
    }

    @Test
    public void testGenerateFromNullObject() {
        EDI997Generator.Result result = EDI997Generator.generate((Object) null);
        assertFalse("Should not succeed with null object", result.isSuccess());
        assertNotNull("Should have error message", result.getError());
    }

    @Test
    public void testResultToString() {
        EDI997Generator.Result result = EDI997Generator.generate((Object) null);
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("EDI997Result"));
    }

    @Test
    public void testAcknowledgeCodes() {
        assertEquals("A", EDI997Generator.ACCEPTED);
        assertEquals("E", EDI997Generator.ACCEPTED_WITH_ERRORS);
        assertEquals("P", EDI997Generator.PARTIALLY_ACCEPTED);
        assertEquals("R", EDI997Generator.REJECTED);
        assertEquals("A", EDI997Generator.TS_ACCEPTED);
        assertEquals("E", EDI997Generator.TS_ACCEPTED_WITH_ERRORS);
        assertEquals("R", EDI997Generator.TS_REJECTED);
    }

    @Test
    public void testGenerateFromNonEdiObject() {
        // A plain string has no ISA/GS envelope fields
        EDI997Generator.Result result = EDI997Generator.generate("just a string");
        assertFalse("Should fail for non-EDI object", result.isSuccess());
        assertNotNull("Should have error", result.getError());
        assertTrue("Error should mention envelope", result.getError().contains("envelope"));
    }

    @Test
    public void testGenerateWithExplicitCodes() {
        EDI997Generator.Result result = EDI997Generator.generate(
                "not-edi", EDI997Generator.REJECTED, EDI997Generator.TS_REJECTED, "12345");
        assertFalse("Should fail for non-EDI object", result.isSuccess());
    }

    @Test
    public void testGenerateFromEmptyUnmarshalResult() {
        EDIUnmarshalResult<Object> unmarshalResult = new EDIUnmarshalResult<>();
        // data is null
        EDI997Generator.Result result = EDI997Generator.generate(unmarshalResult);
        assertFalse("Should fail when data is null", result.isSuccess());
    }
}
