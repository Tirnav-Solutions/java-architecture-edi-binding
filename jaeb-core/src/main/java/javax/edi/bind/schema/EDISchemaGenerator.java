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
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.bind.annotations.elements.EDIElementFormat;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates JSON Schema from EDI model classes.
 * 
 * Introspects EDI annotations (@EDIMessage, @EDISegment, @EDISegmentGroup, @EDIElement)
 * and validation annotations (@NotNull, @Size, @Min, @Max, @DecimalMin, @DecimalMax)
 * to produce a standards-compliant JSON Schema (Draft-07).
 * 
 * Usage:
 *   String schema = EDISchemaGenerator.generateSchema(PurchaseOrder.class);
 *   // or with pretty printing
 *   String schema = EDISchemaGenerator.generateSchema(PurchaseOrder.class, true);
 */
public class EDISchemaGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(EDISchemaGenerator.class);

    private EDISchemaGenerator() {
        // seal
    }

    /**
     * Generates a JSON Schema string for the given EDI message class.
     * 
     * @param clazz the EDI message class (must be annotated with @EDIMessage)
     * @return JSON Schema as a string
     */
    public static String generateSchema(Class<?> clazz) {
        return generateSchema(clazz, true);
    }

    /**
     * Generates a JSON Schema string for the given EDI message class.
     * 
     * @param clazz the EDI message class
     * @param prettyPrint whether to format the output with indentation
     * @return JSON Schema as a string
     */
    public static String generateSchema(Class<?> clazz, boolean prettyPrint) {
        Set<Class<?>> visited = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("title", clazz.getSimpleName());
        
        // Add EDI message metadata
        if (clazz.isAnnotationPresent(EDIMessage.class)) {
            EDIMessage msg = clazz.getAnnotation(EDIMessage.class);
            Map<String, Object> ediMeta = new LinkedHashMap<>();
            ediMeta.put("type", "EDIMessage");
            ediMeta.put("elementDelimiter", String.valueOf(msg.elementDelimiter()));
            ediMeta.put("componentDelimiter", String.valueOf(msg.componentDelimiter()));
            ediMeta.put("segmentDelimiter", String.valueOf(msg.segmentDelimiter()));
            schema.put("x-edi-info", ediMeta);
        }
        
        schema.put("type", "object");
        
        // Build properties and required list
        Map<String, Object> properties = new LinkedHashMap<>();
        java.util.List<String> required = new java.util.ArrayList<>();
        
        buildProperties(clazz, properties, required, visited);
        
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        schema.put("additionalProperties", Boolean.FALSE);
        
        if (prettyPrint) {
            return toJsonPretty(schema, 0);
        } else {
            return toJson(schema);
        }
    }

    private static void buildProperties(Class<?> clazz, Map<String, Object> properties,
            java.util.List<String> required, Set<Class<?>> visited) {
        
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);
        
        for (Field field : clazz.getDeclaredFields()) {
            // Skip synthetic fields
            if (field.isSynthetic()) {
                continue;
            }
            
            Map<String, Object> prop = buildFieldSchema(field, visited);
            properties.put(field.getName(), prop);
            
            if (field.isAnnotationPresent(NotNull.class)) {
                required.add(field.getName());
            }
        }
    }

    private static Map<String, Object> buildFieldSchema(Field field, Set<Class<?>> visited) {
        Map<String, Object> prop = new LinkedHashMap<>();
        Class<?> fieldType = field.getType();
        
        // Check if it's a collection
        if (Collection.class.isAssignableFrom(fieldType)) {
            return buildCollectionSchema(field, visited);
        }
        
        // Check if it's a segment or segment group (nested object)
        if (fieldType.isAnnotationPresent(EDISegment.class) || fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
            return buildObjectSchema(fieldType, field, visited);
        }
        
        // Primitive / simple type
        return buildSimpleFieldSchema(field);
    }

    private static Map<String, Object> buildSimpleFieldSchema(Field field) {
        Map<String, Object> prop = new LinkedHashMap<>();
        Class<?> fieldType = field.getType();
        
        // Determine JSON type - nullable if not @NotNull
        String jsonType = getJsonType(fieldType);
        boolean isRequired = field.isAnnotationPresent(NotNull.class);
        if (!isRequired && !fieldType.isPrimitive()) {
            java.util.List<String> types = new java.util.ArrayList<>();
            types.add(jsonType);
            types.add("null");
            prop.put("type", types);
        } else {
            prop.put("type", jsonType);
        }
        
        // Add EDI element metadata
        if (field.isAnnotationPresent(EDIElement.class)) {
            EDIElement ediElement = field.getAnnotation(EDIElement.class);
            Map<String, Object> ediMeta = new LinkedHashMap<>();
            if (!ediElement.fieldName().isEmpty()) {
                ediMeta.put("fieldName", ediElement.fieldName());
            }
            if (!ediElement.dataElement().isEmpty()) {
                ediMeta.put("dataElement", ediElement.dataElement());
            }
            if (!ediElement.description().isEmpty()) {
                ediMeta.put("description", ediElement.description());
            }
            if (ediElement.required()) {
                ediMeta.put("required", Boolean.TRUE);
            }
            if (!ediMeta.isEmpty()) {
                prop.put("x-edi-element", ediMeta);
            }
        }
        
        // Add format for Date fields
        if (Date.class.isAssignableFrom(fieldType)) {
            if (field.isAnnotationPresent(EDIElementFormat.class)) {
                EDIElementFormat format = field.getAnnotation(EDIElementFormat.class);
                prop.put("format", format.value());
            } else {
                prop.put("format", "date-time");
            }
        }
        
        // @Size constraint
        if (field.isAnnotationPresent(Size.class)) {
            Size size = field.getAnnotation(Size.class);
            if ("string".equals(jsonType)) {
                if (size.min() > 0) {
                    prop.put("minLength", size.min());
                }
                if (size.max() < Integer.MAX_VALUE) {
                    prop.put("maxLength", size.max());
                }
            }
        }
        
        // @Min / @Max constraints
        if (field.isAnnotationPresent(Min.class)) {
            prop.put("minimum", field.getAnnotation(Min.class).value());
        }
        if (field.isAnnotationPresent(Max.class)) {
            prop.put("maximum", field.getAnnotation(Max.class).value());
        }
        
        // @DecimalMin / @DecimalMax constraints
        if (field.isAnnotationPresent(DecimalMin.class)) {
            prop.put("minimum", field.getAnnotation(DecimalMin.class).value());
        }
        if (field.isAnnotationPresent(DecimalMax.class)) {
            prop.put("maximum", field.getAnnotation(DecimalMax.class).value());
        }
        
        // Description from EDI element
        if (field.isAnnotationPresent(EDIElement.class)) {
            EDIElement ediElement = field.getAnnotation(EDIElement.class);
            if (!ediElement.description().isEmpty()) {
                prop.put("description", ediElement.description());
            }
        }
        
        return prop;
    }

    private static Map<String, Object> buildCollectionSchema(Field field, Set<Class<?>> visited) {
        Map<String, Object> prop = new LinkedHashMap<>();
        
        // Mark as nullable if not @NotNull
        boolean isRequired = field.isAnnotationPresent(NotNull.class);
        if (!isRequired) {
            java.util.List<String> types = new java.util.ArrayList<>();
            types.add("array");
            types.add("null");
            prop.put("type", types);
        } else {
            prop.put("type", "array");
        }
        
        // Size constraints on collection
        if (field.isAnnotationPresent(Size.class)) {
            Size size = field.getAnnotation(Size.class);
            if (size.min() > 0) {
                prop.put("minItems", size.min());
            }
            if (size.max() < Integer.MAX_VALUE) {
                prop.put("maxItems", size.max());
            }
        }
        
        // Get collection element type
        Class<?> itemType = null;
        if (field.isAnnotationPresent(EDICollectionType.class)) {
            itemType = field.getAnnotation(EDICollectionType.class).value();
        }
        
        if (itemType != null) {
            if (itemType.isAnnotationPresent(EDISegment.class) || itemType.isAnnotationPresent(EDISegmentGroup.class)) {
                prop.put("items", buildObjectSchema(itemType, null, visited));
            } else {
                Map<String, Object> items = new LinkedHashMap<>();
                items.put("type", getJsonType(itemType));
                prop.put("items", items);
            }
        }
        
        return prop;
    }

    private static Map<String, Object> buildObjectSchema(Class<?> clazz, Field parentField, Set<Class<?>> visited) {
        Map<String, Object> prop = new LinkedHashMap<>();
        
        // Mark as nullable if the parent field is not @NotNull
        boolean isRequired = parentField != null && parentField.isAnnotationPresent(NotNull.class);
        if (!isRequired && parentField != null) {
            java.util.List<String> types = new java.util.ArrayList<>();
            types.add("object");
            types.add("null");
            prop.put("type", types);
        } else {
            prop.put("type", "object");
        }
        prop.put("title", clazz.getSimpleName());
        
        // Add segment/segment group metadata
        if (clazz.isAnnotationPresent(EDISegment.class)) {
            EDISegment seg = clazz.getAnnotation(EDISegment.class);
            Map<String, Object> ediMeta = new LinkedHashMap<>();
            ediMeta.put("type", "EDISegment");
            ediMeta.put("tag", seg.tag());
            prop.put("x-edi-info", ediMeta);
        } else if (clazz.isAnnotationPresent(EDISegmentGroup.class)) {
            EDISegmentGroup grp = clazz.getAnnotation(EDISegmentGroup.class);
            Map<String, Object> ediMeta = new LinkedHashMap<>();
            ediMeta.put("type", "EDISegmentGroup");
            if (!grp.header().isEmpty()) {
                ediMeta.put("header", grp.header());
            }
            if (!grp.footer().isEmpty()) {
                ediMeta.put("footer", grp.footer());
            }
            prop.put("x-edi-info", ediMeta);
        }
        
        // Build nested properties - use a fresh visited set clone to allow same type in different branches
        Map<String, Object> properties = new LinkedHashMap<>();
        java.util.List<String> required = new java.util.ArrayList<>();
        
        Set<Class<?>> branchVisited = new HashSet<>(visited);
        buildProperties(clazz, properties, required, branchVisited);
        
        if (!properties.isEmpty()) {
            prop.put("properties", properties);
        }
        if (!required.isEmpty()) {
            prop.put("required", required);
        }
        prop.put("additionalProperties", Boolean.FALSE);
        
        return prop;
    }

    private static String getJsonType(Class<?> clazz) {
        if (String.class.isAssignableFrom(clazz)) {
            return "string";
        } else if (Date.class.isAssignableFrom(clazz)) {
            return "string";
        } else if (int.class == clazz || Integer.class.isAssignableFrom(clazz)
                || long.class == clazz || Long.class.isAssignableFrom(clazz)
                || short.class == clazz || Short.class.isAssignableFrom(clazz)
                || byte.class == clazz || Byte.class.isAssignableFrom(clazz)) {
            return "integer";
        } else if (float.class == clazz || Float.class.isAssignableFrom(clazz)
                || double.class == clazz || Double.class.isAssignableFrom(clazz)
                || BigDecimal.class.isAssignableFrom(clazz)) {
            return "number";
        } else if (boolean.class == clazz || Boolean.class.isAssignableFrom(clazz)) {
            return "boolean";
        }
        return "string";
    }

    // ========== JSON Serialization (no external dependency) ==========

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
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        String pad = repeat("  ", indent + 1);
        String closePad = repeat("  ", indent);
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append(pad).append("\"").append(escapeJson(entry.getKey())).append("\": ");
            sb.append(toJsonPretty(entry.getValue(), indent + 1));
            i++;
        }
        sb.append("\n").append(closePad).append("}");
        return sb.toString();
    }

    private static String listToJson(java.util.List<?> list, int indent) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        String pad = repeat("  ", indent + 1);
        String closePad = repeat("  ", indent);
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append(pad).append(toJsonPretty(list.get(i), indent + 1));
        }
        sb.append("\n").append(closePad).append("]");
        return sb.toString();
    }

    private static String toJson(Object obj) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":").append(toJson(entry.getValue()));
                i++;
            }
            sb.append("}");
            return sb.toString();
        } else if (obj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Boolean || obj instanceof Number) {
            return obj.toString();
        }
        return "null";
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
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
