package javax.edi.bind.schema;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.bind.annotations.elements.EDIElementFormat;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * Generates a sample/template JSON from EDI model classes.
 * Fills in placeholder values based on field types, validation rules, and EDI metadata.
 * 
 * Useful for:
 * - API documentation: shows consumers exactly what JSON structure to send
 * - Testing: provides a valid JSON template to start from
 * - Onboarding: new integrators can see the expected format immediately
 * 
 * Usage:
 *   String sampleJson = EDISampleGenerator.generateSample(PurchaseOrder.class);
 */
public class EDISampleGenerator {

    private EDISampleGenerator() {
        // seal
    }

    /**
     * Generates a sample JSON string for the given EDI message class.
     * 
     * @param clazz the EDI message class
     * @return sample JSON as a pretty-printed string
     */
    public static String generateSample(Class<?> clazz) {
        Set<Class<?>> visited = new HashSet<>();
        Map<String, Object> sample = buildObjectSample(clazz, visited);
        return toJsonPretty(sample, 0);
    }

    private static Map<String, Object> buildObjectSample(Class<?> clazz, Set<Class<?>> visited) {
        Map<String, Object> obj = new LinkedHashMap<>();
        
        if (visited.contains(clazz)) {
            return obj;
        }
        visited.add(clazz);
        
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            
            Object value = buildFieldSample(field, visited);
            obj.put(field.getName(), value);
        }
        
        return obj;
    }

    private static Object buildFieldSample(Field field, Set<Class<?>> visited) {
        Class<?> fieldType = field.getType();
        
        // Collection
        if (Collection.class.isAssignableFrom(fieldType)) {
            return buildCollectionSample(field, visited);
        }
        
        // Nested segment or segment group
        if (fieldType.isAnnotationPresent(EDISegment.class) || fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
            Set<Class<?>> branchVisited = new HashSet<>(visited);
            return buildObjectSample(fieldType, branchVisited);
        }
        
        // Simple type
        return buildSimpleSample(field);
    }

    private static Object buildCollectionSample(Field field, Set<Class<?>> visited) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        
        Class<?> itemType = null;
        if (field.isAnnotationPresent(EDICollectionType.class)) {
            itemType = field.getAnnotation(EDICollectionType.class).value();
        }
        
        if (itemType != null) {
            if (itemType.isAnnotationPresent(EDISegment.class) || itemType.isAnnotationPresent(EDISegmentGroup.class)) {
                Set<Class<?>> branchVisited = new HashSet<>(visited);
                list.add(buildObjectSample(itemType, branchVisited));
            } else {
                list.add(getPlaceholderValue(itemType, field));
            }
        }
        
        return list;
    }

    private static Object buildSimpleSample(Field field) {
        return getPlaceholderValue(field.getType(), field);
    }

    private static Object getPlaceholderValue(Class<?> type, Field field) {
        String fieldName = field != null ? field.getName() : "";
        
        // Date type
        if (Date.class.isAssignableFrom(type)) {
            if (field != null && field.isAnnotationPresent(EDIElementFormat.class)) {
                String format = field.getAnnotation(EDIElementFormat.class).value();
                if (format.contains("HH") || format.contains("hh")) {
                    return "1200";
                }
                return "20260227";
            }
            return "2026-02-27";
        }
        
        // String type
        if (String.class.isAssignableFrom(type)) {
            // Use @Size to determine length
            if (field != null && field.isAnnotationPresent(Size.class)) {
                Size size = field.getAnnotation(Size.class);
                int len = Math.max(size.min(), 1);
                len = Math.min(len, 10); // cap sample length
                return generateSampleString(fieldName, len);
            }
            
            // Use EDIElement metadata
            if (field != null && field.isAnnotationPresent(EDIElement.class)) {
                EDIElement elem = field.getAnnotation(EDIElement.class);
                if (!elem.fieldName().isEmpty()) {
                    return "<" + elem.fieldName() + ">";
                }
            }
            
            return "<" + fieldName + ">";
        }
        
        // Integer types
        if (int.class == type || Integer.class.isAssignableFrom(type)
                || long.class == type || Long.class.isAssignableFrom(type)) {
            if (field != null && field.isAnnotationPresent(Min.class)) {
                return field.getAnnotation(Min.class).value();
            }
            return 1;
        }
        
        // Decimal types
        if (float.class == type || Float.class.isAssignableFrom(type)
                || double.class == type || Double.class.isAssignableFrom(type)
                || BigDecimal.class.isAssignableFrom(type)) {
            return 0.00;
        }
        
        // Boolean
        if (boolean.class == type || Boolean.class.isAssignableFrom(type)) {
            return false;
        }
        
        return null;
    }

    private static String generateSampleString(String fieldName, int minLen) {
        // Generate a meaningful sample based on common EDI field names
        String lower = fieldName.toLowerCase();
        
        if (lower.contains("code") || lower.contains("type") || lower.contains("qualifier")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < minLen; i++) sb.append("X");
            return sb.toString();
        }
        if (lower.contains("number") || lower.contains("id") || lower.contains("identifier")) {
            return "12345".substring(0, Math.min(5, minLen));
        }
        if (lower.contains("name")) {
            return "SAMPLE NAME";
        }
        if (lower.contains("address") || lower.contains("street")) {
            return "123 MAIN ST";
        }
        if (lower.contains("city")) {
            return "ANYTOWN";
        }
        if (lower.contains("state")) {
            return "CA";
        }
        if (lower.contains("zip") || lower.contains("postal")) {
            return "90210";
        }
        if (lower.contains("country")) {
            return "US";
        }
        
        return "<" + fieldName + ">";
    }

    // ========== JSON Serialization ==========

    @SuppressWarnings("unchecked")
    private static String toJsonPretty(Object obj, int indent) {
        if (obj instanceof Map) {
            return mapToJson((Map<String, Object>) obj, indent);
        } else if (obj instanceof java.util.List) {
            return listToJson((java.util.List<?>) obj, indent);
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Boolean || obj instanceof Number) {
            return obj.toString();
        }
        return "null";
    }

    private static String mapToJson(Map<String, Object> map, int indent) {
        if (map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder();
        String pad = repeat("  ", indent + 1);
        String closePad = repeat("  ", indent);
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) sb.append(",\n");
            sb.append(pad).append("\"").append(escapeJson(entry.getKey())).append("\": ");
            sb.append(toJsonPretty(entry.getValue(), indent + 1));
            i++;
        }
        sb.append("\n").append(closePad).append("}");
        return sb.toString();
    }

    private static String listToJson(java.util.List<?> list, int indent) {
        if (list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        String pad = repeat("  ", indent + 1);
        String closePad = repeat("  ", indent);
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(pad).append(toJsonPretty(list.get(i), indent + 1));
        }
        sb.append("\n").append(closePad).append("]");
        return sb.toString();
    }

    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(str);
        return sb.toString();
    }
}
