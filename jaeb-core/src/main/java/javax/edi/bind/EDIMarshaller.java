package javax.edi.bind;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.bind.util.EDIValidationError;
import javax.edi.bind.util.EDIValidationUtil;
import javax.edi.bind.util.FieldAwareConverter;
import javax.edi.configuration.EDIMessageConfiguration;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EDIMarshaller {

	private static final Logger LOG = LoggerFactory.getLogger(EDIMarshaller.class);

	private EDIMarshaller() {
		// seal
	}
	
	/**
	 * Marshals an EDI object and returns a result wrapper containing:
	 * - The generated EDI string (or null if validation failed)
	 * - List of validation errors (empty if valid)
	 * - Number of EDI segments generated
	 * 
	 * This method NEVER throws an exception. All errors are captured in the result.
	 * 
	 * @param <T> the EDI message type
	 * @param obj the EDI message object to marshal
	 * @return EDIMarshalResult containing the EDI output and any errors
	 */
	public static <T> EDIMarshalResult marshalResult(T obj) {
		EDIMarshalResult result = new EDIMarshalResult();
		try {
			Class<T> clazz = (Class<T>) obj.getClass();
			
			if (!clazz.isAnnotationPresent(EDIMessage.class)) {
				result.setError("Not a EDI Message!");
				return result;
			}
			
			// Validate first - collect errors, don't throw
			EDIValidationError.ValidationResult validationResult = EDIValidationUtil.validateDeepAndCollectErrors(obj);
			result.setValidationResult(validationResult);
			
			if (validationResult.hasErrors()) {
				// Don't generate EDI if validation fails
				return result;
			}
			
			EDIMessage message = clazz.getAnnotation(EDIMessage.class);
			StringWriter writer = new StringWriter();
			processSegmentsAndSegmentGroups(message, obj, writer);
			
			String ediOutput = writer.toString();
			result.setEdiOutput(ediOutput);
			result.setSuccess(true);
			
			// Count segments (each line ending with segment delimiter is a segment)
			int segCount = 0;
			for (char c : ediOutput.toCharArray()) {
				if (c == message.segmentDelimiter()) {
					segCount++;
				}
			}
			result.setSegmentCount(segCount);
			
		} catch (Exception e) {
			result.setError(e.getMessage());
			LOG.error("EDI marshal failed: " + e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * Marshals an EDI object with custom config and returns a result wrapper.
	 * This method NEVER throws an exception. All errors are captured in the result.
	 */
	public static <T> EDIMarshalResult marshalResult(T obj, final EDIMessageConfiguration config) {
		EDIMarshalResult result = new EDIMarshalResult();
		try {
			Class<T> clazz = (Class<T>) obj.getClass();
			
			if (!clazz.isAnnotationPresent(EDIMessage.class)) {
				result.setError("Not a EDI Message!");
				return result;
			}
			
			EDIValidationError.ValidationResult validationResult = EDIValidationUtil.validateDeepAndCollectErrors(obj);
			result.setValidationResult(validationResult);
			
			if (validationResult.hasErrors()) {
				return result;
			}
			
			final EDIMessage message = new EDIMessage() {
				public Class<? extends Annotation> annotationType() { return EDIMessage.class; }
				public char segmentDelimiter() { return config.getSegmentDelimiter(); }
				public char elementDelimiter() { return config.getElementDelimiter(); }
				public char componentDelimiter() { return config.getComponentDelimiter(); }
			};
			
			StringWriter writer = new StringWriter();
			processSegmentsAndSegmentGroups(message, obj, writer);
			
			String ediOutput = writer.toString();
			result.setEdiOutput(ediOutput);
			result.setSuccess(true);
			
			int segCount = 0;
			for (char c : ediOutput.toCharArray()) {
				if (c == message.segmentDelimiter()) {
					segCount++;
				}
			}
			result.setSegmentCount(segCount);
			
		} catch (Exception e) {
			result.setError(e.getMessage());
			LOG.error("EDI marshal failed: " + e.getMessage(), e);
		}
		return result;
	}

    public static <T> void marshal(T obj, Writer writer, final EDIMessageConfiguration config) throws Exception {
    	Class<T> clazz = (Class<T>)obj.getClass(); 
    	
        if(!clazz.isAnnotationPresent(EDIMessage.class)) {
        	throw new EDIMessageException("Not a EDI Message!");
        }
        
        // Validate the object against its annotations
        FieldAwareConverter.validateObject(obj);
        
        final EDIMessage message = new EDIMessage() {
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
        
        processSegmentsAndSegmentGroups(message, obj, writer);
    }
	
    public static <T> void marshal(T obj, Writer writer) throws Exception {
    	Class<T> clazz = (Class<T>)obj.getClass(); 
    	
        if(!clazz.isAnnotationPresent(EDIMessage.class)) {
        	throw new EDIMessageException("Not a EDI Message!");
        }
        
        // Validate the object against its annotations
        FieldAwareConverter.validateObject(obj);
        
        EDIMessage message = clazz.getAnnotation(EDIMessage.class);
        processSegmentsAndSegmentGroups(message, obj, writer);
    }
    
    protected static <T> void processSegmentsAndSegmentGroups(EDIMessage message, T obj, Writer writer) throws Exception {
    	Class<?> clazz = obj.getClass();
    	
    	//now, loop through all segments.
        for(int i=0, j=clazz.getDeclaredFields().length; i<j; i++) {
        	//first, get the object from the message.
        	Field field = clazz.getDeclaredFields()[i];
        	Object fieldObj = PropertyUtils.getProperty(obj, field.getName());
        	
        	if(fieldObj==null) {
        		LOG.debug("Ignoring null object: "+field);
        		continue;
        	}
        	
        	if(Collection.class.isAssignableFrom(fieldObj.getClass())) {
        		Collection<?> collectionObjs = (Collection<?>)fieldObj;

        		if(collectionObjs.size() > 0) {
        			if(!field.isAnnotationPresent(EDICollectionType.class)) {
        				throw new EDIMessageException("@EDICollectionType Annotation is required for field: "+field.getName());
        			}
        			Class<?> testClass = field.getAnnotation(EDICollectionType.class).value();
        			
        			if(testClass.isAnnotationPresent(EDISegment.class)) {
        				writeMultiSegments(message, collectionObjs, writer);
            		}
        			else if(testClass.isAnnotationPresent(EDISegmentGroup.class)) {
        				writeMultiSegmentGroups(message, collectionObjs, writer);
        			}
        			else {
        				throw new EDIMessageException("EDI Message Class Field is not Annotated as Segment of SegmentGroup.");
        			}
        		}
        	}
        	else {
        		if(fieldObj.getClass().isAnnotationPresent(EDISegment.class)) {
        			writeSegment(message, fieldObj, writer);
        		}
    			else if(fieldObj.getClass().isAnnotationPresent(EDISegmentGroup.class)) {
    				writeSingleSegmentGroup(message, fieldObj, writer);
    			}
    			else {
    				throw new EDIMessageException("EDI Message Class Field["+field.getName()+"] is not Annotated as Segment of SegmentGroup.");
    			}
        	}
        }   
    }

    protected static <T> void writeSingleSegmentGroup(EDIMessage message, T obj, Writer writer) throws Exception {
		Class<T> clazz = (Class<T>)obj.getClass();

		EDISegmentGroup segmentGroup = clazz.getAnnotation(EDISegmentGroup.class);
		
		if(obj == null) {
    		return;
    	}
		
        if(!clazz.isAnnotationPresent(EDISegmentGroup.class)) {
        	throw new EDIMessageException("Expected EDI Segment Group.");
        }
        
        if(StringUtils.isNotBlank(segmentGroup.header())) {
        	//print the header / footer.
        	writer.append(segmentGroup.header()).append(message.segmentDelimiter());
        }

		writeSegmentGroup(message, obj, writer);
        
        if(StringUtils.isNotBlank(segmentGroup.footer())) {
        	//print the header / footer.
        	writer.append(segmentGroup.footer()+message.segmentDelimiter());
        }
    }
    
    protected static <T> void writeMultiSegmentGroups(EDIMessage message, T obj, Writer writer) throws Exception {
		Collection<?> collectionObjs = (Collection<?>)obj;
		Object testObject = collectionObjs.iterator().next();
		Class<T> clazz = (Class<T>)testObject.getClass();

		EDISegmentGroup segmentGroup = clazz.getAnnotation(EDISegmentGroup.class);
		
		if(obj == null || collectionObjs.size() == 0) {
    		return;
    	}
		
        if(!clazz.isAnnotationPresent(EDISegmentGroup.class)) {
        	throw new EDIMessageException("Expected EDI Segment Group.");
        }
        
        if(StringUtils.isNotBlank(segmentGroup.header())) {
        	//print the header / footer for each group in the collection.
        	for(Object collectionObj : collectionObjs) {
        		writer.append(segmentGroup.header());
        		writer.append(message.segmentDelimiter());
        		writeSegmentGroup(message, collectionObj, writer);
        		if(StringUtils.isNotBlank(segmentGroup.footer())) {
        			writer.append(segmentGroup.footer());
        			writer.append(message.segmentDelimiter());
        		}
        	}
        }
        else {
        	for(Object collectionObj : collectionObjs) {
        		writeSegmentGroup(message, collectionObj, writer);
        	}
        }
    }
    
    protected static <T> void writeSegmentGroup(EDIMessage message, T obj, Writer writer) throws Exception {
    	processSegmentsAndSegmentGroups(message, obj, writer);
    }
    
    protected static <T> void writeMultiSegments(EDIMessage message, T obj, Writer writer) throws Exception {
    	Collection<?> collectionObjs = (Collection<?>)obj;
		
    	for(Object collectionObj : collectionObjs) {
				writeSegment(message, collectionObj, writer);
		}
    }
    
    protected static <T> void writeSegment(EDIMessage message, T obj, Writer writer) throws Exception {
    	if(obj == null) {
    		return;
    	}
    	
    	Class<T> clazz = (Class<T>)obj.getClass(); 
    	
        if(!clazz.isAnnotationPresent(EDISegment.class)) {
        	throw new EDIMessageException("Expected EDI Segment.");
        }
        
        //add the segment code.
        EDISegment segment = clazz.getAnnotation(EDISegment.class);
        writer.append(segment.tag()+message.elementDelimiter());
        
        //now, loop through all segments.
        for(int i=0, j=clazz.getDeclaredFields().length; i<j; i++) {
        	Field field = clazz.getDeclaredFields()[i];
        	EDIElement f = field.getAnnotation(EDIElement.class);
        	String fieldName = field.getName();
        	Class<?> fieldType = field.getType();
        	
        	if(i > 0) {
        		writer.append(message.elementDelimiter());
        	}
        	
        	writeField(message, field, obj, writer);
        }  
        
        writer.append(message.segmentDelimiter()).append("\n");
    }
    
    protected static <T> void writeField(EDIMessage message, Field field, T obj, Writer writer) throws Exception {
    	Object value = PropertyUtils.getProperty(obj, field.getName());
    	if(value == null) {
    		return;
    	}
    	
    	if(field.isAnnotationPresent(EDIComponent.class)) {
    		EDIComponent ediComponent = field.getAnnotation(EDIComponent.class); 
    		char componentDelimiter = ediComponent.delimiter() == Character.UNASSIGNED ? message.componentDelimiter() : ediComponent.delimiter(); 
    		
    		//treat as component.
    		if(Collection.class.isAssignableFrom(value.getClass())) {
    			StringBuilder componentField = new StringBuilder();
    			
    			Collection<?> collectionObjs = (Collection<?>)value;
    			int i=0;
    			for(Object collectionObj : collectionObjs) {
        			//get the component...
        			if(i>0) {
        				componentField.append(componentDelimiter);
        			}
        			componentField.append(FieldAwareConverter.convertToString(field, collectionObj));
        			i++;
        		}
    			
    			writer.append(componentField.toString());
    		}
    	}
    	else {
    		String val = FieldAwareConverter.convertToString(field, value);    	
        	writer.append(val);
    	}
    }
  
}
