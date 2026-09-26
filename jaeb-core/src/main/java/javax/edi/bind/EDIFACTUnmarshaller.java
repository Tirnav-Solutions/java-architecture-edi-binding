package javax.edi.bind;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDIFACTMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.bind.util.CollectionFactory;
import javax.edi.bind.util.EDIValidationError;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unmarshaller for UN/EDIFACT messages.
 *
 * <p>Handles EDIFACT-specific features:</p>
 * <ul>
 *   <li>UNA service string advice (overrides default delimiters)</li>
 *   <li>UNB interchange header / UNZ interchange trailer</li>
 *   <li>UNH message header / UNT message trailer</li>
 *   <li>Default delimiters: {@code +} element, {@code :} component, {@code '} segment</li>
 *   <li>Release character {@code ?} for escaping delimiters in data</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 *   EDIFACTUnmarshalResult&lt;DelforD96A&gt; result =
 *       EDIFACTUnmarshaller.unmarshal(DelforD96A.class, reader);
 *   if (result.hasErrors()) { ... }
 *   DelforD96A delfor = result.getData();
 * </pre>
 */
public class EDIFACTUnmarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(EDIFACTUnmarshaller.class);

    private EDIFACTUnmarshaller() {}

    // ======================== Public API ========================

    /**
     * Unmarshal an EDIFACT message. Never throws ? all errors are captured in result.
     */
    public static <T> EDIFACTUnmarshalResult<T> unmarshal(Class<T> clz, Reader reader) {
        EDIFACTUnmarshalResult<T> result = new EDIFACTUnmarshalResult<>();
        long start = System.currentTimeMillis();

        try {
            if (!clz.isAnnotationPresent(EDIFACTMessage.class)) {
                result.setParseError("Class is not annotated with @EDIFACTMessage: " + clz.getName());
                return result;
            }

            EDIFACTMessage meta = clz.getAnnotation(EDIFACTMessage.class);
            String raw = readFully(reader);

            // Detect delimiters (UNA or defaults)
            Delimiters delims = detectDelimiters(raw, meta);
            result.setDelimiters(delims);

            // Split into segments
            List<String> segments = splitSegments(raw, delims);
            result.setSegmentCount(segments.size());

            // Extract envelope info
            extractEnvelope(segments, delims, result);

            // Parse message body into model
            T obj = clz.newInstance();
            parseSegments(obj, clz, segments, delims, result);
            result.setData(obj);
            result.setParsed(true);

        } catch (Exception e) {
            LOG.error("EDIFACT unmarshal failed: {}", e.getMessage(), e);
            result.setParseError(e.getMessage());
        } finally {
            result.setParseTimeMillis(System.currentTimeMillis() - start);
        }
        return result;
    }

    // ======================== UNA Detection ========================

    static Delimiters detectDelimiters(String raw, EDIFACTMessage meta) {
        char comp = meta.componentDelimiter();
        char elem = meta.elementDelimiter();
        char decimal = meta.decimalNotation();
        char release = meta.releaseCharacter();
        char seg = meta.segmentDelimiter();

        // UNA service string advice: tag (from annotation) + 6 characters
        // e.g. UNA:+.? '
        //          0123 5   (pos 4 = reserved space, pos 5 = segment terminator)
        String ssaTag = meta.serviceStringAdvice();
        int ssaLen = ssaTag.length() + 6; // tag + 6 delimiter chars
        if (raw.length() >= ssaLen && raw.startsWith(ssaTag)) {
            int offset = ssaTag.length();
            comp    = raw.charAt(offset);
            elem    = raw.charAt(offset + 1);
            decimal = raw.charAt(offset + 2);
            release = raw.charAt(offset + 3);
            // offset + 4 = reserved (usually space)
            seg     = raw.charAt(offset + 5);
            LOG.debug("{} detected: comp={} elem={} dec={} rel={} seg={}",
                    ssaTag, comp, elem, decimal, release, seg);
        }

        return new Delimiters(comp, elem, seg, release, decimal);
    }

    // ======================== Segment Splitting ========================

    static List<String> splitSegments(String raw, Delimiters d) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean released = false;

        // Skip UNA if present
        int startIdx = 0;
        if (raw.startsWith("UNA")) {
            startIdx = 9; // skip "UNA" + 6 chars
        }

        for (int i = startIdx; i < raw.length(); i++) {
            char c = raw.charAt(i);

            if (released) {
                current.append(c);
                released = false;
                continue;
            }

            if (c == d.release) {
                released = true;
                continue;
            }

            if (c == d.segment) {
                String seg = current.toString().trim();
                if (!seg.isEmpty()) {
                    segments.add(seg);
                }
                current.setLength(0);
                continue;
            }

            // Ignore newlines/carriage returns within segments
            if (c == '\n' || c == '\r') continue;

            current.append(c);
        }

        // Last segment without terminator
        String last = current.toString().trim();
        if (!last.isEmpty()) {
            segments.add(last);
        }

        return segments;
    }

    // ======================== Envelope Extraction ========================

    static <T> void extractEnvelope(List<String> segments, Delimiters d, EDIFACTUnmarshalResult<T> result) {
        for (String seg : segments) {
            String tag = getSegmentTag(seg, d);
            String[] elements = splitElements(seg, d);

            if ("UNB".equals(tag) && elements.length > 1) {
                result.setSenderIdentification(safeGet(elements, 2));
                result.setReceiverIdentification(safeGet(elements, 3));
                result.setInterchangeReference(safeGet(elements, 5));
            } else if ("UNH".equals(tag) && elements.length > 1) {
                result.setMessageReference(safeGet(elements, 1));
                // UNH+1+DELFOR:D:96A:UN
                if (elements.length > 2) {
                    String[] msgId = splitComponents(elements[2], d);
                    result.setMessageType(safeGet(msgId, 0));
                    result.setMessageVersion(safeGet(msgId, 1));
                    result.setMessageRelease(safeGet(msgId, 2));
                    result.setControllingAgency(safeGet(msgId, 3));
                }
            }
        }
    }

    // ======================== Message Body Parsing ========================

    static <T> void parseSegments(T obj, Class<?> clz, List<String> segments,
                                   Delimiters d, EDIFACTUnmarshalResult<?> result) {
        Field[] fields = clz.getDeclaredFields();
        int fieldIdx = 0;  // tracks how far we've advanced through the fields
        int segIdx = 0;

        while (segIdx < segments.size()) {
            String currentTag = getSegmentTag(segments.get(segIdx), d);

            // Skip envelope/trailer segments (UNB, UNZ, UNT)
            if ("UNT".equals(currentTag) || "UNZ".equals(currentTag)) {
                segIdx++;
                continue;
            }

            // Try to find a matching field — first forward from fieldIdx, then backtrack
            boolean matched = false;

            // Pass 1: forward scan from current field position
            int matchResult = tryMatchField(obj, fields, fieldIdx, fields.length,
                    segments, segIdx, currentTag, d, result);
            if (matchResult >= 0) {
                segIdx = matchResult;
                matched = true;
            }

            // Pass 2: if forward scan failed, backtrack to try earlier Collection fields
            //         (only Collection fields, since single fields are already populated)
            if (!matched) {
                matchResult = tryMatchCollectionField(obj, fields, 0, fieldIdx,
                        segments, segIdx, currentTag, d, result);
                if (matchResult >= 0) {
                    segIdx = matchResult;
                    matched = true;
                }
            }

            if (!matched) {
                // No field matched this segment — skip it
                LOG.debug("Skipping unmatched EDIFACT segment: {}", currentTag);
                segIdx++;
            }
        }
    }

    /**
     * Try to match the current segment tag against fields in [fromIdx, toIdx).
     * Returns the new segIdx if matched, or -1 if no match.
     */
    private static <T> int tryMatchField(T obj, Field[] fields, int fromIdx, int toIdx,
                                          List<String> segments, int segIdx,
                                          String currentTag, Delimiters d,
                                          EDIFACTUnmarshalResult<?> result) {
        for (int fi = fromIdx; fi < toIdx; fi++) {
            Field field = fields[fi];
            field.setAccessible(true);

            Class<?> fieldType = field.getType();
            boolean isCollection = Collection.class.isAssignableFrom(fieldType);
            if (isCollection) {
                fieldType = getCollectionElementType(field);
                if (fieldType == null) continue;
            }

            if (fieldType.isAnnotationPresent(EDISegment.class)) {
                EDISegment segAnno = fieldType.getAnnotation(EDISegment.class);
                String expectedTag = segAnno.tag();

                if (expectedTag.equals(currentTag)) {
                    if (isCollection) {
                        Collection<Object> collection = ensureCollection(obj, field);
                        while (segIdx < segments.size()) {
                            String tag = getSegmentTag(segments.get(segIdx), d);
                            if (!expectedTag.equals(tag)) break;
                            Object segObj = parseSegmentFields(fieldType, segments.get(segIdx), d, result);
                            if (segObj != null) collection.add(segObj);
                            segIdx++;
                        }
                    } else {
                        Object segObj = parseSegmentFields(fieldType, segments.get(segIdx), d, result);
                        setField(obj, field, segObj);
                        segIdx++;
                    }
                    return segIdx;
                }
            } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                EDISegmentGroup groupAnno = fieldType.getAnnotation(EDISegmentGroup.class);
                String headerTag = groupAnno.header();

                if (headerTag.equals(currentTag)) {
                    if (isCollection) {
                        Collection<Object> collection = ensureCollection(obj, field);
                        while (segIdx < segments.size()) {
                            String tag = getSegmentTag(segments.get(segIdx), d);
                            if (!headerTag.equals(tag)) break;
                            Object groupObj;
                            try {
                                groupObj = fieldType.newInstance();
                            } catch (Exception e) {
                                break;
                            }
                            segIdx = parseGroup(groupObj, fieldType, segments, segIdx, d, result);
                            collection.add(groupObj);
                        }
                    } else {
                        try {
                            Object groupObj = fieldType.newInstance();
                            segIdx = parseGroup(groupObj, fieldType, segments, segIdx, d, result);
                            setField(obj, field, groupObj);
                        } catch (Exception e) {
                            LOG.warn("Failed to create segment group: {}", e.getMessage());
                        }
                    }
                    return segIdx;
                }
            }
        }
        return -1;  // no match
    }

    /**
     * Try to match the current segment tag against Collection fields only in [fromIdx, toIdx).
     * Used for backtracking — we only add to lists, never overwrite single-value fields.
     */
    private static <T> int tryMatchCollectionField(T obj, Field[] fields, int fromIdx, int toIdx,
                                                    List<String> segments, int segIdx,
                                                    String currentTag, Delimiters d,
                                                    EDIFACTUnmarshalResult<?> result) {
        for (int fi = fromIdx; fi < toIdx; fi++) {
            Field field = fields[fi];
            field.setAccessible(true);

            Class<?> fieldType = field.getType();
            boolean isCollection = Collection.class.isAssignableFrom(fieldType);
            if (!isCollection) continue;

            fieldType = getCollectionElementType(field);
            if (fieldType == null) continue;

            if (fieldType.isAnnotationPresent(EDISegment.class)) {
                EDISegment segAnno = fieldType.getAnnotation(EDISegment.class);
                if (segAnno.tag().equals(currentTag)) {
                    Collection<Object> collection = ensureCollection(obj, field);
                    while (segIdx < segments.size()) {
                        String tag = getSegmentTag(segments.get(segIdx), d);
                        if (!segAnno.tag().equals(tag)) break;
                        Object segObj = parseSegmentFields(fieldType, segments.get(segIdx), d, result);
                        if (segObj != null) collection.add(segObj);
                        segIdx++;
                    }
                    return segIdx;
                }
            }
        }
        return -1;  // no match
    }

    /**
     * Parse a segment group. Uses flexible matching: for each incoming segment,
     * scans ALL fields (not just forward) to find a match. This allows
     * interleaved segments like DTM?RFF?RFF?DTM within a group.
     *
     * <p>Group parsing stops when:</p>
     * <ul>
     *   <li>We encounter the header tag again (next group instance)</li>
     *   <li>We encounter an envelope/trailer tag (UNS, UNT, UNZ)</li>
     *   <li>We encounter a tag that is NOT in this group's field set
     *       and is NOT in any nested group's field set</li>
     *   <li>We run out of segments</li>
     * </ul>
     */
    private static int parseGroup(Object groupObj, Class<?> groupClass, List<String> segments,
                                   int startIdx, Delimiters d, EDIFACTUnmarshalResult<?> result) {
        Field[] fields = groupClass.getDeclaredFields();
        int segIdx = startIdx;
        String headerTag = "";

        // Determine the header tag of this group
        if (groupClass.isAnnotationPresent(EDISegmentGroup.class)) {
            headerTag = groupClass.getAnnotation(EDISegmentGroup.class).header();
        }

        // Build set of tags that this group can contain (for termination check)
        java.util.Set<String> groupTags = new java.util.LinkedHashSet<>();
        groupTags.add(headerTag);
        for (Field f : fields) {
            Class<?> ft = f.getType();
            if (Collection.class.isAssignableFrom(ft)) {
                ft = getCollectionElementType(f);
                if (ft == null) continue;
            }
            if (ft.isAnnotationPresent(EDISegment.class)) {
                groupTags.add(ft.getAnnotation(EDISegment.class).tag());
            } else if (ft.isAnnotationPresent(EDISegmentGroup.class)) {
                // Add the nested group's header tag and all its child tags
                groupTags.add(ft.getAnnotation(EDISegmentGroup.class).header());
                for (Field nf : ft.getDeclaredFields()) {
                    Class<?> nft = nf.getType();
                    if (Collection.class.isAssignableFrom(nft)) {
                        nft = getCollectionElementType(nf);
                        if (nft == null) continue;
                    }
                    if (nft.isAnnotationPresent(EDISegment.class)) {
                        groupTags.add(nft.getAnnotation(EDISegment.class).tag());
                    }
                }
            }
        }

        // First segment should be the header — parse it as the header field
        if (segIdx < segments.size()) {
            String currentTag = getSegmentTag(segments.get(segIdx), d);
            if (headerTag.equals(currentTag)) {
                // Find and populate the header field
                for (Field field : fields) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    if (fieldType.isAnnotationPresent(EDISegment.class)) {
                        EDISegment segAnno = fieldType.getAnnotation(EDISegment.class);
                        if (headerTag.equals(segAnno.tag())) {
                            Object segObj = parseSegmentFields(fieldType, segments.get(segIdx), d, result);
                            setField(groupObj, field, segObj);
                            segIdx++;
                            break;
                        }
                    }
                }
            }
        }

        // Now parse remaining segments in a flexible manner
        while (segIdx < segments.size()) {
            String currentTag = getSegmentTag(segments.get(segIdx), d);

            // Stop conditions
            if ("UNT".equals(currentTag) || "UNZ".equals(currentTag) || "UNS".equals(currentTag)) {
                break;
            }
            // If we hit the header tag again, this is the next group instance — stop
            if (headerTag.equals(currentTag)) {
                break;
            }
            // If the tag is not recognized by this group, stop
            if (!groupTags.contains(currentTag)) {
                break;
            }

            // Try to match against all fields (flexible order)
            boolean matched = false;
            for (Field field : fields) {
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                boolean isCollection = Collection.class.isAssignableFrom(fieldType);
                if (isCollection) {
                    fieldType = getCollectionElementType(field);
                    if (fieldType == null) continue;
                }

                if (fieldType.isAnnotationPresent(EDISegment.class)) {
                    EDISegment segAnno = fieldType.getAnnotation(EDISegment.class);
                    String expectedTag = segAnno.tag();

                    if (expectedTag.equals(currentTag)) {
                        if (isCollection) {
                            Collection<Object> collection = ensureCollection(groupObj, field);
                            while (segIdx < segments.size()) {
                                String tag = getSegmentTag(segments.get(segIdx), d);
                                if (!expectedTag.equals(tag)) break;
                                Object segObj = parseSegmentFields(fieldType, segments.get(segIdx), d, result);
                                if (segObj != null) collection.add(segObj);
                                segIdx++;
                            }
                        } else {
                            Object segObj = parseSegmentFields(fieldType, segments.get(segIdx), d, result);
                            setField(groupObj, field, segObj);
                            segIdx++;
                        }
                        matched = true;
                        break;
                    }
                } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                    EDISegmentGroup nestedGroupAnno = fieldType.getAnnotation(EDISegmentGroup.class);
                    String nestedHeaderTag = nestedGroupAnno.header();

                    if (nestedHeaderTag.equals(currentTag)) {
                        if (isCollection) {
                            Collection<Object> collection = ensureCollection(groupObj, field);
                            while (segIdx < segments.size()) {
                                String tag = getSegmentTag(segments.get(segIdx), d);
                                if (!nestedHeaderTag.equals(tag)) break;
                                Object nestedGroupObj;
                                try {
                                    nestedGroupObj = fieldType.newInstance();
                                } catch (Exception e) {
                                    break;
                                }
                                segIdx = parseGroup(nestedGroupObj, fieldType, segments, segIdx, d, result);
                                collection.add(nestedGroupObj);
                            }
                        } else {
                            try {
                                Object nestedGroupObj = fieldType.newInstance();
                                segIdx = parseGroup(nestedGroupObj, fieldType, segments, segIdx, d, result);
                                setField(groupObj, field, nestedGroupObj);
                            } catch (Exception e) {
                                LOG.warn("Failed to create nested segment group: {}", e.getMessage());
                            }
                        }
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched) {
                // Segment tag is in groupTags but didn't match a field — skip
                LOG.debug("Skipping unmatched segment within group {}: {}", groupClass.getSimpleName(), currentTag);
                segIdx++;
            }
        }
        return segIdx;
    }

    // ======================== Segment Field Parsing ========================

    private static Object parseSegmentFields(Class<?> segClass, String rawSegment,
                                              Delimiters d, EDIFACTUnmarshalResult<?> result) {
        try {
            Object obj = segClass.newInstance();
            String[] elements = splitElements(rawSegment, d);
            // elements[0] = tag, elements[1..n] = data elements

            Field[] fields = segClass.getDeclaredFields();
            int elemIdx = 1; // skip tag
            int fieldIdx = 0;

            while (fieldIdx < fields.length && elemIdx < elements.length) {
                Field field = fields[fieldIdx];
                field.setAccessible(true);

                if (field.isAnnotationPresent(EDIComponent.class)) {
                    // @EDIComponent: split the current element by component delimiter
                    // and assign consecutive @EDIComponent fields from the components
                    String value = elements[elemIdx];
                    String[] components = splitComponents(value, d);
                    int compIdx = 0;

                    while (fieldIdx < fields.length) {
                        Field compField = fields[fieldIdx];
                        if (!compField.isAnnotationPresent(EDIComponent.class)) break;
                        compField.setAccessible(true);
                        if (compIdx < components.length) {
                            setStringOrPrimitive(obj, compField, components[compIdx]);
                        }
                        compIdx++;
                        fieldIdx++;
                    }
                    elemIdx++;
                } else if (field.isAnnotationPresent(EDIElement.class)) {
                    String value = elements[elemIdx];

                    // If the field type is a composite (has fields with @EDIElement),
                    // split by component delimiter
                    if (hasAnnotatedFields(field.getType())) {
                        String[] components = splitComponents(value, d);
                        Object composite = field.getType().newInstance();
                        Field[] compFields = field.getType().getDeclaredFields();
                        for (int ci = 0; ci < compFields.length && ci < components.length; ci++) {
                            compFields[ci].setAccessible(true);
                            setStringOrPrimitive(composite, compFields[ci], components[ci]);
                        }
                        field.set(obj, composite);
                    } else {
                        setStringOrPrimitive(obj, field, value);
                    }
                    elemIdx++;
                    fieldIdx++;
                } else {
                    fieldIdx++;
                }
            }
            return obj;
        } catch (Exception e) {
            LOG.warn("Failed to parse EDIFACT segment {}: {}", segClass.getSimpleName(), e.getMessage());
            if (result != null) {
                result.addError(new EDIValidationError(
                        segClass.getSimpleName(), "Parse error: " + e.getMessage(), rawSegment));
            }
            return null;
        }
    }

    // ======================== Utilities ========================

    private static String readFully(Reader reader) throws Exception {
        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = br.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    static String getSegmentTag(String segment, Delimiters d) {
        int idx = segment.indexOf(d.element);
        if (idx > 0) return segment.substring(0, idx);
        return segment;
    }

    static String[] splitElements(String segment, Delimiters d) {
        return splitWithRelease(segment, d.element, d.release);
    }

    static String[] splitComponents(String element, Delimiters d) {
        return splitWithRelease(element, d.component, d.release);
    }

    private static String[] splitWithRelease(String input, char delimiter, char release) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean released = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (released) {
                current.append(c);
                released = false;
                continue;
            }
            if (c == release) {
                released = true;
                continue;
            }
            if (c == delimiter) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static String safeGet(String[] arr, int idx) {
        return (arr != null && idx < arr.length) ? arr[idx] : null;
    }

    private static void setStringOrPrimitive(Object obj, Field field, String value) throws Exception {
        if (value == null || value.isEmpty()) return;
        Class<?> type = field.getType();
        if (type == String.class) {
            field.set(obj, value);
        } else if (type == int.class || type == Integer.class) {
            field.set(obj, Integer.parseInt(value.trim()));
        } else if (type == long.class || type == Long.class) {
            field.set(obj, Long.parseLong(value.trim()));
        } else if (type == double.class || type == Double.class) {
            field.set(obj, Double.parseDouble(value.trim()));
        } else {
            field.set(obj, value);
        }
    }

    private static boolean hasAnnotatedFields(Class<?> clz) {
        if (clz == null || clz == String.class || clz.isPrimitive()) return false;
        for (Field f : clz.getDeclaredFields()) {
            if (f.isAnnotationPresent(EDIElement.class)) return true;
        }
        return false;
    }

    private static void setField(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            LOG.warn("Failed to set field {}: {}", field.getName(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> ensureCollection(Object obj, Field field) {
        try {
            field.setAccessible(true);
            Collection<Object> col = (Collection<Object>) field.get(obj);
            if (col == null) {
                col = new ArrayList<>();
                field.set(obj, col);
            }
            return col;
        } catch (Exception e) {
            LOG.warn("Failed to get/create collection for {}: {}", field.getName(), e.getMessage());
            return new ArrayList<>();
        }
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

    // ======================== Delimiters DTO ========================

    public static class Delimiters {
        public final char component;
        public final char element;
        public final char segment;
        public final char release;
        public final char decimal;

        public Delimiters(char component, char element, char segment, char release, char decimal) {
            this.component = component;
            this.element = element;
            this.segment = segment;
            this.release = release;
            this.decimal = decimal;
        }

        @Override
        public String toString() {
            return "Delimiters{comp=" + component + ", elem=" + element +
                    ", seg=" + segment + ", rel=" + release + ", dec=" + decimal + "}";
        }
    }
}
