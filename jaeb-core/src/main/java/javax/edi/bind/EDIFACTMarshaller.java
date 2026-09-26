package javax.edi.bind;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDIFACTMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marshaller for EDIFACT messages.
 *
 * <p>Generates a valid EDIFACT interchange from an {@code @EDIFACTMessage} model object,
 * including UNA service string, UNB/UNZ envelope, UNH/UNT message envelope,
 * and all segment data.</p>
 *
 * <p>All delimiters and the service string advice tag are read dynamically
 * from the {@code @EDIFACTMessage} annotation on the model class.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   String edi = EDIFACTMarshaller.marshal(delforD96A);
 * </pre>
 */
public class EDIFACTMarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(EDIFACTMarshaller.class);

    private EDIFACTMarshaller() {}

    /**
     * Marshal an EDIFACT message object to an EDI string.
     * Delimiters and service string advice tag are read from the {@code @EDIFACTMessage}
     * annotation on the model class.
     *
     * @param obj the message object annotated with {@code @EDIFACTMessage}
     * @return the EDIFACT EDI string
     */
    public static <T> String marshal(T obj) {
        Class<?> clazz = obj.getClass();
        if (!clazz.isAnnotationPresent(EDIFACTMessage.class)) {
            throw new IllegalArgumentException("Not an @EDIFACTMessage class: " + clazz.getName());
        }
        EDIFACTMessage meta = clazz.getAnnotation(EDIFACTMessage.class);
        return marshal(obj, meta.componentDelimiter(), meta.elementDelimiter(),
                meta.decimalNotation(), meta.releaseCharacter(), meta.segmentDelimiter());
    }

    /**
     * Marshal an EDIFACT message object with custom delimiters.
     */
    public static <T> String marshal(T obj, char comp, char elem, char dec, char rel, char seg) {
        try {
            Class<?> clazz = obj.getClass();
            if (!clazz.isAnnotationPresent(EDIFACTMessage.class)) {
                throw new IllegalArgumentException("Not an @EDIFACTMessage class: " + clazz.getName());
            }

            EDIFACTMessage meta = clazz.getAnnotation(EDIFACTMessage.class);
            StringWriter writer = new StringWriter();

            // UNA service string advice ? tag from annotation, delimiters from parameters
            writer.append(meta.serviceStringAdvice())
                  .append(comp).append(elem).append(dec).append(rel).append(' ').append(seg)
                  .append('\n');

            // Walk the fields of the message model and write each segment
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue;

                Class<?> fieldType = field.getType();

                if (List.class.isAssignableFrom(fieldType)) {
                    // Repeating segments or groups
                    List<?> items = (List<?>) value;
                    for (Object item : items) {
                        if (item == null) continue;
                        if (item.getClass().isAnnotationPresent(EDISegment.class)) {
                            writeSegment(item, writer, comp, elem, seg);
                        } else if (item.getClass().isAnnotationPresent(EDISegmentGroup.class)) {
                            writeGroup(item, writer, comp, elem, seg);
                        }
                    }
                } else if (fieldType.isAnnotationPresent(EDISegment.class)) {
                    writeSegment(value, writer, comp, elem, seg);
                } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                    writeGroup(value, writer, comp, elem, seg);
                }
            }

            return writer.toString();
        } catch (Exception e) {
            LOG.error("EDIFACT marshal failed: {}", e.getMessage(), e);
            throw new RuntimeException("EDIFACT marshal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Write all segments within a segment group (recursively handles nested groups).
     */
    private static void writeGroup(Object groupObj, Writer writer, char comp, char elem, char seg) throws Exception {
        Class<?> clazz = groupObj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(groupObj);
            if (value == null) continue;

            Class<?> fieldType = field.getType();

            if (List.class.isAssignableFrom(fieldType)) {
                List<?> items = (List<?>) value;
                for (Object item : items) {
                    if (item == null) continue;
                    if (item.getClass().isAnnotationPresent(EDISegment.class)) {
                        writeSegment(item, writer, comp, elem, seg);
                    } else if (item.getClass().isAnnotationPresent(EDISegmentGroup.class)) {
                        writeGroup(item, writer, comp, elem, seg);
                    }
                }
            } else if (fieldType.isAnnotationPresent(EDISegment.class)) {
                writeSegment(value, writer, comp, elem, seg);
            } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                writeGroup(value, writer, comp, elem, seg);
            }
        }
    }

    /**
     * Write a single EDIFACT segment.
     */
    private static void writeSegment(Object obj, Writer writer, char comp, char elem, char seg) throws Exception {
        Class<?> clazz = obj.getClass();
        EDISegment segAnno = clazz.getAnnotation(EDISegment.class);
        writer.append(segAnno.tag());

        Field[] fields = clazz.getDeclaredFields();
        int fieldIdx = 0;

        while (fieldIdx < fields.length) {
            Field field = fields[fieldIdx];
            field.setAccessible(true);

            if (field.isAnnotationPresent(EDIComponent.class)) {
                // Start a new element (element delimiter)
                writer.append(elem);

                // Collect all consecutive @EDIComponent fields into one composite
                StringBuilder composite = new StringBuilder();
                boolean first = true;
                while (fieldIdx < fields.length) {
                    Field compField = fields[fieldIdx];
                    if (!compField.isAnnotationPresent(EDIComponent.class)) break;
                    compField.setAccessible(true);
                    if (!first) composite.append(comp);
                    Object val = compField.get(obj);
                    if (val != null) composite.append(val.toString());
                    first = false;
                    fieldIdx++;
                }
                // Trim trailing component separators
                String compStr = composite.toString();
                while (compStr.endsWith(String.valueOf(comp))) {
                    compStr = compStr.substring(0, compStr.length() - 1);
                }
                writer.append(compStr);

            } else if (field.isAnnotationPresent(EDIElement.class)) {
                writer.append(elem);
                Object val = field.get(obj);
                if (val != null) writer.append(val.toString());
                fieldIdx++;
            } else {
                fieldIdx++;
            }
        }

        writer.append(seg).append('\n');
    }
}