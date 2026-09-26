package javax.edi.bind;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDAMessage;
import javax.edi.bind.annotations.VDARecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marshaller for VDA fixed-width messages (4905, 4984, etc.).
 *
 * <p>Generates a valid VDA message string from a {@code @VDAMessage} model object,
 * padding each record to the configured record length (default 128 chars).</p>
 *
 * <p>Two output modes:</p>
 * <ul>
 *   <li><b>Standard</b> ({@link #marshal(Object)}): pure fixed-width positional format
 *       as per VDA specification — no separators.</li>
 *   <li><b>Readable</b> ({@link #marshalReadable(Object, String)}): fields separated by a
 *       user-specified delimiter (e.g. {@code "|"}) for debugging/display.</li>
 * </ul>
 *
 * <p>If {@code @VDAMessage(fieldSeparator = "|")} is set on the model class,
 * {@link #marshal(Object)} will automatically use that separator.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   // Standard VDA fixed-width
 *   String vda = VDAMarshaller.marshal(vda4905Message);
 *
 *   // Human-readable with pipe separators
 *   String readable = VDAMarshaller.marshalReadable(vda4905Message, "|");
 * </pre>
 */
public class VDAMarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(VDAMarshaller.class);

    private VDAMarshaller() {}

    /**
     * Marshal a VDA message object to a VDA string.
     *
     * <p>If the model's {@code @VDAMessage} annotation specifies a non-empty
     * {@code fieldSeparator}, fields will be separated by that character.
     * Otherwise, standard fixed-width positional format is used.</p>
     *
     * @param obj the message object annotated with {@code @VDAMessage}
     * @return the VDA message string
     */
    public static <T> String marshal(T obj) {
        Class<?> clazz = obj.getClass();
        if (!clazz.isAnnotationPresent(VDAMessage.class)) {
            throw new IllegalArgumentException("Not a @VDAMessage class: " + clazz.getName());
        }
        VDAMessage meta = clazz.getAnnotation(VDAMessage.class);
        String sep = meta.fieldSeparator();
        if (sep != null && !sep.isEmpty()) {
            return marshalReadable(obj, sep);
        }
        return marshalFixedWidth(obj);
    }

    /**
     * Marshal a VDA message to standard fixed-width positional format (no separators).
     *
     * @param obj the message object annotated with {@code @VDAMessage}
     * @return the VDA message string in fixed-width format
     */
    public static <T> String marshalFixedWidth(T obj) {
        try {
            Class<?> clazz = obj.getClass();
            if (!clazz.isAnnotationPresent(VDAMessage.class)) {
                throw new IllegalArgumentException("Not a @VDAMessage class: " + clazz.getName());
            }

            VDAMessage meta = clazz.getAnnotation(VDAMessage.class);
            int recordLength = meta.recordLength();

            StringWriter writer = new StringWriter();

            // Walk the fields of the message model in declaration order
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue;

                Class<?> fieldType = field.getType();

                if (List.class.isAssignableFrom(fieldType)) {
                    List<?> items = (List<?>) value;
                    for (Object item : items) {
                        if (item != null && item.getClass().isAnnotationPresent(VDARecord.class)) {
                            writeFixedWidthRecord(item, writer, recordLength);
                        }
                    }
                } else if (fieldType.isAnnotationPresent(VDARecord.class)) {
                    writeFixedWidthRecord(value, writer, recordLength);
                }
            }

            return writer.toString();
        } catch (Exception e) {
            LOG.error("VDA marshal failed: {}", e.getMessage(), e);
            throw new RuntimeException("VDA marshal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Marshal a VDA message with a field separator between each field value.
     *
     * <p>Useful for debugging, logging, or interchange with systems that
     * expect delimited rather than positional VDA data.</p>
     *
     * @param obj       the message object annotated with {@code @VDAMessage}
     * @param separator the separator string to insert between fields (e.g. "|", "\t")
     * @return the VDA message string with separators
     */
    public static <T> String marshalReadable(T obj, String separator) {
        try {
            Class<?> clazz = obj.getClass();
            if (!clazz.isAnnotationPresent(VDAMessage.class)) {
                throw new IllegalArgumentException("Not a @VDAMessage class: " + clazz.getName());
            }

            StringWriter writer = new StringWriter();

            // Walk the fields of the message model in declaration order
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue;

                Class<?> fieldType = field.getType();

                if (List.class.isAssignableFrom(fieldType)) {
                    List<?> items = (List<?>) value;
                    for (Object item : items) {
                        if (item != null && item.getClass().isAnnotationPresent(VDARecord.class)) {
                            writeSeparatedRecord(item, writer, separator);
                        }
                    }
                } else if (fieldType.isAnnotationPresent(VDARecord.class)) {
                    writeSeparatedRecord(value, writer, separator);
                }
            }

            return writer.toString();
        } catch (Exception e) {
            LOG.error("VDA marshal (readable) failed: {}", e.getMessage(), e);
            throw new RuntimeException("VDA marshal (readable) failed: " + e.getMessage(), e);
        }
    }

    /**
     * Write a single VDA record (fixed-width, padded to recordLength).
     */
    private static void writeFixedWidthRecord(Object obj, StringWriter writer, int recordLength) throws Exception {
        Class<?> clazz = obj.getClass();
        char[] buffer = new char[recordLength];
        // Fill with spaces
        for (int i = 0; i < recordLength; i++) buffer[i] = ' ';

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(VDAField.class)) continue;
            field.setAccessible(true);
            VDAField vf = field.getAnnotation(VDAField.class);
            Object val = field.get(obj);
            if (val == null) continue;

            String str = val.toString();
            int start = vf.start();
            int len = vf.length();

            // Write characters into the buffer at the specified position
            for (int i = 0; i < len && i < str.length(); i++) {
                if (start + i < recordLength) {
                    buffer[start + i] = str.charAt(i);
                }
            }
        }

        writer.append(new String(buffer)).append("\n");
    }

    /**
     * Write a single VDA record with separators between field values.
     * Fields are written in positional order (sorted by start position).
     * Each field value is padded to its defined length.
     */
    private static void writeSeparatedRecord(Object obj, StringWriter writer, String separator) throws Exception {
        Class<?> clazz = obj.getClass();

        // Collect annotated fields sorted by start position
        List<FieldEntry> entries = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(VDAField.class)) continue;
            field.setAccessible(true);
            entries.add(new FieldEntry(field, field.getAnnotation(VDAField.class)));
        }
        Collections.sort(entries, Comparator.comparingInt(e -> e.anno.start()));

        boolean first = true;
        for (FieldEntry entry : entries) {
            Object val = entry.field.get(obj);
            String str = (val != null) ? val.toString() : "";
            int len = entry.anno.length();

            // Pad or truncate to the defined field length
            if (str.length() < len) {
                str = String.format("%-" + len + "s", str);
            } else if (str.length() > len) {
                str = str.substring(0, len);
            }

            if (!first) writer.append(separator);
            writer.append(str);
            first = false;
        }
        writer.append("\n");
    }

    /** Helper: pairs a Field with its @VDAField annotation for sorting. */
    private static class FieldEntry {
        final Field field;
        final VDAField anno;
        FieldEntry(Field field, VDAField anno) {
            this.field = field;
            this.anno = anno;
        }
    }
}