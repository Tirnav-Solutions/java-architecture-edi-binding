package javax.edi.bind;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDAMessage;
import javax.edi.bind.annotations.VDARecord;
import javax.edi.bind.util.EDIValidationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unmarshaller for VDA (Verband der Automobilindustrie) fixed-width messages.
 *
 * <p>VDA messages consist of 128-character fixed-width records.
 * Each record is identified by a record type code (typically positions 0-2).
 * Fields are extracted by positional offset and length.</p>
 *
 * <p>Supported VDA standards:</p>
 * <ul>
 *   <li>VDA 4905 — Delivery schedule (Lieferabruf)</li>
 *   <li>VDA 4984 — Dispatch advice (Lieferschein)</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 *   VDAUnmarshalResult&lt;VDA4905Message&gt; result =
 *       VDAUnmarshaller.unmarshal(VDA4905Message.class, reader);
 * </pre>
 */
public class VDAUnmarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(VDAUnmarshaller.class);

    private VDAUnmarshaller() {}

    /**
     * Unmarshal a VDA fixed-width message. Never throws — all errors are captured in result.
     */
    public static <T> VDAUnmarshalResult<T> unmarshal(Class<T> clz, Reader reader) {
        VDAUnmarshalResult<T> result = new VDAUnmarshalResult<>();
        long start = System.currentTimeMillis();

        try {
            if (!clz.isAnnotationPresent(VDAMessage.class)) {
                result.setParseError("Class is not annotated with @VDAMessage: " + clz.getName());
                return result;
            }

            VDAMessage meta = clz.getAnnotation(VDAMessage.class);
            int expectedLength = meta.recordLength();

            // Read all lines
            List<String> lines = readLines(reader);
            result.setRecordCount(lines.size());

            if (lines.isEmpty()) {
                result.setParseError("Empty VDA message");
                return result;
            }

            // Build a map of record type -> Field
            T obj = clz.newInstance();
            parseRecords(obj, clz, lines, expectedLength, result);
            result.setData(obj);
            result.setParsed(true);

        } catch (Exception e) {
            LOG.error("VDA unmarshal failed: {}", e.getMessage(), e);
            result.setParseError(e.getMessage());
        } finally {
            result.setParseTimeMillis(System.currentTimeMillis() - start);
        }
        return result;
    }

    private static <T> void parseRecords(T obj, Class<?> clz, List<String> lines,
                                          int expectedLength, VDAUnmarshalResult<?> result) {
        Field[] fields = clz.getDeclaredFields();

        // Build a map: VDARecord type -> list of fields on the parent class
        Map<String, List<FieldInfo>> recordFieldMap = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            Class<?> recordType = fieldType;

            if (Collection.class.isAssignableFrom(fieldType)) {
                recordType = getCollectionElementType(field);
                if (recordType == null) continue;
            }

            if (recordType.isAnnotationPresent(VDARecord.class)) {
                String recType = recordType.getAnnotation(VDARecord.class).type();
                recordFieldMap.computeIfAbsent(recType, k -> new ArrayList<>())
                        .add(new FieldInfo(field, recordType, Collection.class.isAssignableFrom(fieldType)));
            }
        }

        // Count records by type
        Map<String, Integer> recordStats = new LinkedHashMap<>();

        for (String line : lines) {
            if (line.isEmpty()) continue;

            // Pad line to expected length if shorter
            String paddedLine = line;
            if (paddedLine.length() < expectedLength) {
                paddedLine = String.format("%-" + expectedLength + "s", paddedLine);
            }

            // Extract record type (typically positions 0-2)
            String recType = extractRecordType(paddedLine);
            recordStats.merge(recType, 1, Integer::sum);

            List<FieldInfo> fieldInfos = recordFieldMap.get(recType);
            if (fieldInfos == null) {
                LOG.debug("Unhandled VDA record type: {}", recType);
                continue;
            }

            for (FieldInfo fi : fieldInfos) {
                try {
                    Object record = parseRecord(fi.recordClass, paddedLine, result);
                    if (record == null) continue;

                    if (fi.isCollection) {
                        Collection<Object> col = ensureCollection(obj, fi.field);
                        col.add(record);
                    } else {
                        fi.field.set(obj, record);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to set VDA record field {}: {}", fi.field.getName(), e.getMessage());
                }
            }
        }

        result.setRecordStats(recordStats);
    }

    /**
     * Parse a single VDA record line into an object using @VDAField annotations.
     */
    private static Object parseRecord(Class<?> recClass, String line, VDAUnmarshalResult<?> result) {
        try {
            Object obj = recClass.newInstance();
            Field[] fields = recClass.getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(VDAField.class)) continue;
                field.setAccessible(true);

                VDAField vf = field.getAnnotation(VDAField.class);
                int start = vf.start();
                int end = Math.min(start + vf.length(), line.length());

                if (start >= line.length()) {
                    if (vf.required()) {
                        result.addError(new EDIValidationError(
                                recClass.getSimpleName() + "." + field.getName(),
                                "Required field position " + start + " exceeds record length " + line.length(),
                                null));
                    }
                    continue;
                }

                String rawValue = line.substring(start, end).trim();

                if (rawValue.isEmpty()) {
                    if (vf.required()) {
                        result.addError(new EDIValidationError(
                                recClass.getSimpleName() + "." + field.getName(),
                                "Required field is empty", null));
                    }
                    continue;
                }

                setFieldValue(obj, field, rawValue, vf.format(), result);
            }
            return obj;
        } catch (Exception e) {
            LOG.warn("Failed to parse VDA record {}: {}", recClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private static void setFieldValue(Object obj, Field field, String value,
                                       String format, VDAUnmarshalResult<?> result) throws Exception {
        Class<?> type = field.getType();

        if (type == String.class) {
            field.set(obj, value);
        } else if (type == int.class || type == Integer.class) {
            try {
                field.set(obj, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                field.set(obj, 0);
                result.addError(new EDIValidationError(
                        field.getName(), "Invalid numeric value", value));
            }
        } else if (type == long.class || type == Long.class) {
            try {
                field.set(obj, Long.parseLong(value));
            } catch (NumberFormatException e) {
                field.set(obj, 0L);
                result.addError(new EDIValidationError(
                        field.getName(), "Invalid numeric value", value));
            }
        } else if (type == double.class || type == Double.class) {
            try {
                field.set(obj, Double.parseDouble(value));
            } catch (NumberFormatException e) {
                field.set(obj, 0.0);
            }
        } else {
            field.set(obj, value);
        }
    }

    /**
     * Extract the record type code. VDA records typically have the type in positions 0-2.
     */
    private static String extractRecordType(String line) {
        if (line.length() >= 3) {
            return line.substring(0, 3).trim();
        }
        return line.trim();
    }

    private static List<String> readLines(Reader reader) throws Exception {
        BufferedReader br = new BufferedReader(reader);
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> ensureCollection(Object obj, Field field) throws Exception {
        field.setAccessible(true);
        Collection<Object> col = (Collection<Object>) field.get(obj);
        if (col == null) {
            col = new ArrayList<>();
            field.set(obj, col);
        }
        return col;
    }

    private static Class<?> getCollectionElementType(Field field) {
        java.lang.reflect.Type genericType = field.getGenericType();
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return null;
    }

    private static class FieldInfo {
        final Field field;
        final Class<?> recordClass;
        final boolean isCollection;

        FieldInfo(Field field, Class<?> recordClass, boolean isCollection) {
            this.field = field;
            this.recordClass = recordClass;
            this.isCollection = isCollection;
        }
    }
}
