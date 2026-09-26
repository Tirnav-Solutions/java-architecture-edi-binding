package javax.edi.bind.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a VDA fixed-width record type.
 * Each VDA record starts with a record type code at a known position.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VDARecord {
    /** Record type code (e.g. "511", "512", "721"). */
    String type();
}
