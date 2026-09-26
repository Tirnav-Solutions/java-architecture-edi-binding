package javax.edi.bind.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a VDA fixed-width message model.
 * VDA messages use positional (fixed-width) record formats,
 * NOT delimiter-based segments like X12 or EDIFACT.
 *
 * Each record is identified by a record type code in the first few characters.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VDAMessage {
    /** VDA standard number (e.g. "4905", "4984"). */
    String standard();

    /** Record length (VDA records are typically 128 characters). */
    int recordLength() default 128;

    /**
     * Optional field separator character inserted between fields in marshalled output.
     * Default is empty (no separator — standard VDA fixed-width format).
     * Set to e.g. "|" for a human-readable display mode.
     */
    String fieldSeparator() default "";
}
