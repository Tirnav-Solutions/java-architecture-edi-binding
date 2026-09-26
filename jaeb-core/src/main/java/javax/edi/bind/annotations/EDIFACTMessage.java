package javax.edi.bind.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an EDIFACT message model.
 * EDIFACT uses different default delimiters than X12:
 *   element separator = '+'
 *   component separator = ':'
 *   segment terminator = '\''
 *   release character = '?'
 *
 * The UNA service string advice segment (if present) overrides these defaults.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EDIFACTMessage {
    /** EDIFACT message type (e.g. "DELFOR", "DESADV", "ORDERS"). */
    String type();

    /** EDIFACT directory version (e.g. "D96A", "D04A"). */
    String version() default "";

    char elementDelimiter() default '+';
    char componentDelimiter() default ':';
    char segmentDelimiter() default '\'';
    char releaseCharacter() default '?';
    char decimalNotation() default '.';

    /** Service string advice tag (always "UNA" per ISO 9735). */
    String serviceStringAdvice() default "UNA";
}
