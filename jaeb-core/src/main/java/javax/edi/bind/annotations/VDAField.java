package javax.edi.bind.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in a VDA record as a fixed-width positional field.
 * Fields are extracted by start position and length from the record line.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VDAField {
    /** 0-based start position within the record line. */
    int start();

    /** Length in characters. */
    int length();

    /** Human-readable description. */
    String description() default "";

    /** Whether this field is required. */
    boolean required() default false;

    /**
     * Format hint for parsing (e.g. "N" for numeric, "AN" for alphanumeric,
     * "D" for date YYMMDD, "D8" for date YYYYMMDD).
     */
    String format() default "AN";
}
