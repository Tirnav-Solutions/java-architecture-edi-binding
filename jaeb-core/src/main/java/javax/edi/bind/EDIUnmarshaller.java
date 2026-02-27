package javax.edi.bind;

import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.bind.hierarchy.HierarchyReference;
import javax.edi.bind.hierarchy.HierarchyUtil;
import javax.edi.bind.util.BufferedSegmentIterator;
import javax.edi.bind.util.CollectionFactory;
import javax.edi.bind.util.FieldAwareConverter;
import javax.edi.bind.util.SegmentIterator;
import javax.edi.configuration.EDIMessageConfiguration;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDIUnmarshaller
{
    private static final Logger LOG;
    
    static {
        LOG = LoggerFactory.getLogger((Class)EDIUnmarshaller.class);
    }
    
    private EDIUnmarshaller() {
    }
    
    public static <T> T unmarshal(final Class<T> clz, final Reader reader, final EDIMessageConfiguration config) throws Exception {
        if (config == null) {
            //throw new EDIMessageException("EDI Configuration not set.");
        }
        final EDIMessage ediMessage = new EDIMessage() {
            public Class<? extends Annotation> annotationType() {
                return EDIMessage.class;
            }
            
            public char segmentDelimiter() {
                return config.getSegmentDelimiter();
            }
            
            public char elementDelimiter() {
                return config.getElementDelimiter();
            }
            
            public char componentDelimiter() {
                return config.getComponentDelimiter();
            }
        };
        return parseEDIMessage(clz, reader, ediMessage);
    }
    
    public static <T> T unmarshal(final Class<T> clz, final Reader reader) throws Exception {
        return (T)parseEDIMessage((Class<Object>)clz, reader);
    }
    
    protected static <T> T parseEDIMessage(final Class<T> clz, final Reader reader) throws EDIMessageException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, ConversionException {
        if (!clz.isAnnotationPresent(EDIMessage.class)) {
            throw new EDIMessageException("Not EDI Message Class.");
        }
        final EDIMessage ediMessage = clz.getAnnotation(EDIMessage.class);
        return parseEDIMessage(clz, reader, ediMessage);
    }
    
    private static <T> T parseEDIMessage(final Class<T> clz, final Reader reader, final EDIMessage ediMessage) throws InstantiationException, IllegalAccessException, EDIMessageException, InvocationTargetException, ClassNotFoundException, ConversionException {
        final BufferedSegmentIterator bufferedIterator = new BufferedSegmentIterator(new SegmentIterator(reader, ediMessage.segmentDelimiter(), true));
        final Field[] fields = clz.getDeclaredFields();
        final Iterator<Field> fieldIterator = Arrays.asList(fields).iterator();
        final T obj = clz.newInstance();
        final Stack<HierarchyReference> stack = new Stack<HierarchyReference>();
        while (fieldIterator.hasNext() && bufferedIterator.hasNext()) {
            parseEDISegmentOrSegmentGroup(ediMessage, obj, fieldIterator, bufferedIterator, stack);
        }
        if (bufferedIterator.hasNext()) {
            final StringBuilder sb = new StringBuilder();
            while (bufferedIterator.hasNext()) {
                final String segment = bufferedIterator.next();
                sb.append("Unhandled segment: " + segment + "\n");
            }
            throw new EDIMessageException("Unparsed segments remain: " + sb.toString());
        }
        return obj;
    }
    
    protected static <T> void parseEDISegmentOrSegmentGroup(final EDIMessage ediMessage, final T object, final Iterator<Field> fieldIterator, final BufferedSegmentIterator segmentIterator, final Stack<HierarchyReference> hierarchy) throws EDIMessageException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, ConversionException {
        if (!fieldIterator.hasNext()) {
            throw new EDIMessageException("No more fields to read.");
        }
        if (!segmentIterator.hasNext()) {
            return;
        }
        final String line = segmentIterator.peek();
        if (line.startsWith("HL")) {
            System.out.println("asd");
        }
        final FieldMatch fm = advanceToMatch(ediMessage, fieldIterator, line);
        if (fm != null) {
            segmentIterator.next();
            final Class<?> fieldType = (Class<?>)getEDISegmentOrGroupType(fm.getField());
            if (fieldType.isAnnotationPresent(EDISegment.class)) {
                processSegment(ediMessage, object, segmentIterator, fm, hierarchy);
            }
            else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                processSegmentGroup(ediMessage, object, segmentIterator, fm, hierarchy);
            }
        }
    }
    
    protected static <T> void processSegmentGroup(final EDIMessage ediMessage, final T object, final BufferedSegmentIterator segmentIterator, final FieldMatch fm, final Stack<HierarchyReference> hierarchy) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, ConversionException, EDIMessageException {
        EDIUnmarshaller.LOG.debug("Object: " + ReflectionToStringBuilder.toString((Object)object));
        EDIUnmarshaller.LOG.debug("Field: " + fm.getField().getName());
        final Class<?> segmentGroupClass = (Class<?>)getEDISegmentOrGroupType(fm.getField());
        if (HierarchyUtil.segmentGroupHasHierarchyReference(segmentGroupClass)) {
            EDIUnmarshaller.LOG.info("Segment group has hierachy: " + segmentGroupClass.getCanonicalName());
        }
        if (!segmentGroupClass.isAnnotationPresent(EDISegmentGroup.class)) {
            throw new EDIMessageException("Segment Group should have annotation.");
        }
        EDIUnmarshaller.LOG.debug("Segment Group Type: " + segmentGroupClass);
        String line = fm.getLine();
        final EDISegmentGroup es = segmentGroupClass.getAnnotation(EDISegmentGroup.class);
        if (!StringUtils.equals(es.header(), line)) {
            EDIUnmarshaller.LOG.debug("Adding to Look Ahead: " + line);
            segmentIterator.add(line);
        }
        if (Collection.class.isAssignableFrom(fm.getField().getType())) {
            final Collection obj = CollectionFactory.newInstance(fm.getField().getType());
            BeanUtils.setProperty((Object)object, fm.getField().getName(), (Object)obj);
            final String segmentTag = getSegmentTag(fm.getField(), true);
            do {
                EDIUnmarshaller.LOG.debug("Looping to collect Collection of Segment Groups");
                final Field[] fields = segmentGroupClass.getDeclaredFields();
                final Iterator<Field> fieldIterator = Arrays.asList(fields).iterator();
                final Object collectionObj = segmentGroupClass.newInstance();
                while (fieldIterator.hasNext() && segmentIterator.hasNext()) {
                    parseEDISegmentOrSegmentGroup(ediMessage, collectionObj, fieldIterator, segmentIterator, hierarchy);
                }
                obj.add(collectionObj);
                final String nextLine = segmentIterator.peek();
                final String candidateTag = StringUtils.substringBefore(nextLine, CharUtils.toString(ediMessage.elementDelimiter()));
                if (!StringUtils.equals(segmentTag, candidateTag)) {//Add Check
                    break;
                }else if("HL".equalsIgnoreCase(candidateTag) && !nextLine.equalsIgnoreCase(line)) {
                	String[] prev = line.split(Pattern.quote(String.valueOf(ediMessage.elementDelimiter())));
                	String[] next = nextLine.split(Pattern.quote(String.valueOf(ediMessage.elementDelimiter())));
                    // Get the third element (index 2) from the array
                    String prevThirdElement = prev.length > 2 ? prev[2] : "";
                    String nextThirdElement = next.length > 2 ? next[2] : "";
                    if(!prevThirdElement.isEmpty() && nextThirdElement.isEmpty()) {
                    	break;
                    }
                }
            } while (segmentIterator.hasNext());
        }
        else {
            final Field[] fields2 = segmentGroupClass.getDeclaredFields();
            final Iterator<Field> fieldIterator2 = Arrays.asList(fields2).iterator();
            final Object obj2 = segmentGroupClass.newInstance();
            while (fieldIterator2.hasNext() && segmentIterator.hasNext()) {
                parseEDISegmentOrSegmentGroup(ediMessage, obj2, fieldIterator2, segmentIterator, hierarchy);
            }
            BeanUtils.setProperty((Object)object, fm.getField().getName(), obj2);
        }
        if (StringUtils.isNotBlank(es.header())) {
            line = segmentIterator.peek();
            if (StringUtils.endsWith(es.footer(), line)) {
                segmentIterator.next();
            }
        }
    }
    
    protected static <T> void processSegment(final EDIMessage ediMessage, final T object, final BufferedSegmentIterator segmentIterator, final FieldMatch fm, final Stack<HierarchyReference> hierarchy) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, ConversionException {
        if (Collection.class.isAssignableFrom(fm.getField().getType())) {
            final Collection obj = CollectionFactory.newInstance(fm.getField().getType());
            BeanUtils.setProperty((Object)object, fm.getField().getName(), (Object)obj);
            final Class<?> collectionClass = (Class<?>)getCollectionType(fm.getField());
            if (collectionClass.isAnnotationPresent(EDISegment.class)) {
                final EDISegment es = collectionClass.getAnnotation(EDISegment.class);
                final Object matchObject = collectionClass.newInstance();
                parseEDISegmentFields(ediMessage, matchObject, fm.getLine());
                obj.add(matchObject);
                final Queue<String> segments = queueLinesForType(ediMessage, es, segmentIterator);
                for (final String segment : segments) {
                    final Object collectionObject = collectionClass.newInstance();
                    parseEDISegmentFields(ediMessage, collectionObject, segment);
                    obj.add(collectionObject);
                }
            }
        }
        else {
            final Object obj2 = fm.getField().getType().newInstance();
            BeanUtils.setProperty((Object)object, fm.getField().getName(), obj2);
            parseEDISegmentFields(ediMessage, obj2, fm.getLine());
            if (HierarchyUtil.isHierarchyReference(fm.getField().getType())) {
                final HierarchyReference ref = HierarchyUtil.generateHierarchyReference(obj2);
                System.out.println("Hierarchy.");
                hierarchy.add(ref);
            }
        }
    }
    
    protected static Queue<String> queueLinesForType(final EDIMessage ediMessage, final EDISegment segment, final BufferedSegmentIterator segmentIterator) {
        final Queue<String> segments = new LinkedList<String>();
        while (segmentIterator.hasNext()) {
            final String candidate = segmentIterator.peek();
            final String tag = StringUtils.substringBefore(candidate, CharUtils.toString(ediMessage.elementDelimiter()));
            if (!StringUtils.equals(segment.tag(), tag)) {
                break;
            }
            segments.add(segmentIterator.next());
        }
        return segments;
    }
    
    protected static FieldMatch advanceToMatch(final EDIMessage ediMessage, final Iterator<Field> fieldIterator, final String line) {
        final String ediSegmentTag = StringUtils.substringBefore(line, CharUtils.toString(ediMessage.elementDelimiter()));
        while (fieldIterator.hasNext()) {
            final Field field = fieldIterator.next();
            if (matchesSegment(field, ediSegmentTag)) {
                EDIUnmarshaller.LOG.debug("SegmentMatch[" + ediSegmentTag + " -> " + field.getType().getName() + "] :: " + line);
                return new FieldMatch(field, line);
            }
            if (!EDIUnmarshaller.LOG.isDebugEnabled()) {
                continue;
            }
            EDIUnmarshaller.LOG.debug("Field: " + field + " does not match: " + line);
        }
        return null;
    }
    
    protected static String getSegmentTag(final Field field, final boolean inner) {
        Class<?> clz = field.getType();
        if (Collection.class.isAssignableFrom(clz)) {
            clz = (Class<?>)getCollectionType(field);
        }
        if (clz.isAnnotationPresent(EDISegment.class)) {
            final EDISegment es = clz.getAnnotation(EDISegment.class);
            return es.tag();
        }
        if (clz.isAnnotationPresent(EDISegmentGroup.class)) {
            final EDISegmentGroup esg = clz.getAnnotation(EDISegmentGroup.class);
            final String ediSegmentGroupTag = esg.header();
            if (!inner && StringUtils.isNotBlank(ediSegmentGroupTag)) {
                return ediSegmentGroupTag;
            }
            if (clz.getDeclaredFields().length > 0) {
                return getSegmentTag(clz.getDeclaredFields()[0], false);
            }
        }
        return null;
    }
    
    protected static boolean matchesSegment(final Field field, final String segmentTag) {
        Class<?> clz = field.getType();
        if (Collection.class.isAssignableFrom(clz)) {
            clz = (Class<?>)getCollectionType(field);
        }
        if (clz.isAnnotationPresent(EDISegment.class)) {
            final EDISegment es = clz.getAnnotation(EDISegment.class);
            if (StringUtils.equals(segmentTag, es.tag())) {
                return true;
            }
        }
        else if (clz.isAnnotationPresent(EDISegmentGroup.class)) {
            final EDISegmentGroup esg = clz.getAnnotation(EDISegmentGroup.class);
            final String ediSegmentGroupTag = esg.header();
            if (StringUtils.isNotBlank(ediSegmentGroupTag)) {
                if (StringUtils.equals(segmentTag, ediSegmentGroupTag)) {
                    return true;
                }
            }
            else {
                Field[] declaredFields;
                for (int length = (declaredFields = clz.getDeclaredFields()).length, i = 0; i < length; ++i) {
                    final Field decField = declaredFields[i];
                    final boolean match = matchesSegment(decField, segmentTag);
                    if (match) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    protected static <T> void parseEDISegmentFields(final EDIMessage ediMessage, final Object segment, final String segmentLine) throws IllegalAccessException, InvocationTargetException, ClassNotFoundException, ConversionException, InstantiationException {
        if (EDIUnmarshaller.LOG.isDebugEnabled()) {
            EDIUnmarshaller.LOG.debug("Before Field Values: " + ReflectionToStringBuilder.toString(segment));
            EDIUnmarshaller.LOG.debug("Segment Values: " + segmentLine);
        }
        final StrTokenizer tokenizer = new StrTokenizer(segmentLine, ediMessage.elementDelimiter());
        tokenizer.setEmptyTokenAsNull(true);
        tokenizer.setIgnoreEmptyTokens(false);
        tokenizer.next();
        final Iterator<Field> fieldIterator = Arrays.asList(segment.getClass().getDeclaredFields()).iterator();
        while (tokenizer.hasNext() && fieldIterator.hasNext()) {
            final Field field = fieldIterator.next();
            final String val = tokenizer.nextToken();
            if (field.isAnnotationPresent(EDIComponent.class)) {
                final EDIComponent ediComponent = field.getAnnotation(EDIComponent.class);
                final Collection obj = CollectionFactory.newInstance(field.getType());
                final Class objType = getCollectionType(field);
                final char componentDelimiter = (ediComponent.delimiter() == '\0') ? ediMessage.componentDelimiter() : ediComponent.delimiter();
                final StrTokenizer componentTokenizer = new StrTokenizer(val, componentDelimiter);
                componentTokenizer.setEmptyTokenAsNull(true);
                componentTokenizer.setIgnoreEmptyTokens(false);
                while (componentTokenizer.hasNext()) {
                    final String component = componentTokenizer.nextToken();
                    final Object fieldObj = objType.cast(FieldAwareConverter.convertFromString((Class<Object>)objType, field, component));
                    obj.add(fieldObj);
                }
                BeanUtils.setProperty(segment, field.getName(), (Object)obj);
            }
            else if (val == null) {
                EDIUnmarshaller.LOG.debug("  " + field.getName() + " -> null");
            }
            else {
                try {
                    final Object fieldObj2 = FieldAwareConverter.convertFromString(field.getType(), field, val);
                    EDIUnmarshaller.LOG.debug("  " + field.getName() + " -> " + val);
                    BeanUtils.setProperty(segment, field.getName(), fieldObj2);
                }
                catch (Exception e) {
                	e.printStackTrace();
                    throw new ConversionException("Exception setting: " + segment.getClass() + "." + field.getName() + " with value: " + val, (Throwable)e);
                }
            }
        }
        if (EDIUnmarshaller.LOG.isDebugEnabled()) {
            EDIUnmarshaller.LOG.debug("After Field Values: " + ReflectionToStringBuilder.toString(segment));
        }
    }
    
    protected static Class getEDISegmentOrGroupType(final Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            return getCollectionType(field);
        }
        return field.getType();
    }
    
    protected static Class getCollectionType(final Field field) {
        if (field.isAnnotationPresent(EDICollectionType.class)) {
            return field.getAnnotation(EDICollectionType.class).value();
        }
        EDIUnmarshaller.LOG.warn("Ensure the field: " + field.toString() + " contains the @EDICollectionType annotation.");
        return null;
    }
    
    protected static class FieldMatch
    {
        private final Field field;
        private final String line;
        
        public FieldMatch(final Field field, final String line) {
            this.field = field;
            this.line = line;
        }
        
        public Field getField() {
            return this.field;
        }
        
        public String getLine() {
            return this.line;
        }
        
        @Override
        public String toString() {
            return "FieldMatch [field=" + this.field + ", line=" + this.line + "]";
        }
    }
}