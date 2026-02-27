package javax.edi.bind.util;

import java.io.File;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.edi.bind.EDIMessageException;
import javax.edi.bind.annotations.elements.EDIElementFormat;
import javax.edi.bind.annotations.elements.EDIElementFormats;
import javax.edi.bind.annotations.elements.EDILocale;
import javax.edi.bind.annotations.elements.EDITimezone;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BigIntegerConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.ByteConverter;
import org.apache.commons.beanutils.converters.CalendarConverter;
import org.apache.commons.beanutils.converters.CharacterConverter;
import org.apache.commons.beanutils.converters.ClassConverter;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FileConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.beanutils.converters.NumberConverter;
import org.apache.commons.beanutils.converters.ShortConverter;
import org.apache.commons.beanutils.converters.SqlDateConverter;
import org.apache.commons.beanutils.converters.SqlTimeConverter;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.apache.commons.beanutils.converters.URLConverter;
import org.apache.commons.beanutils.locale.converters.DateLocaleConverter;
import org.apache.commons.beanutils.locale.converters.StringLocaleConverter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldAwareConverter {
	private static final Logger LOG = LoggerFactory.getLogger(FieldAwareConverter.class);
	
    public static Converter getConverterType(Class<?> inputClass) {
    	if(inputClass.isAssignableFrom(java.util.Date.class)) {
    		return new DateConverter();
    	}else if(inputClass.isAssignableFrom(java.sql.Date.class)) {
    		return new SqlDateConverter();
    	}else if(inputClass.isAssignableFrom(java.sql.Time.class)) {
    		return new SqlTimeConverter();
    	}else if(inputClass.isAssignableFrom(Timestamp.class)) {
    		return new SqlTimestampConverter();
    	}else if(inputClass.isAssignableFrom(Calendar.class)) {
    		return new CalendarConverter();
    	}else if(inputClass.isAssignableFrom(BigDecimal.class)) {
    		return new BigDecimalConverter();
    	}else if(inputClass.isAssignableFrom(BigInteger.class)) {
    		return new BigIntegerConverter();
    	}else if(inputClass.isAssignableFrom(Boolean.class)) {
    		return new BooleanConverter();
    	}else if(inputClass.isAssignableFrom(Byte.class)) {
    		return new ByteConverter();
    	}else if(inputClass.isAssignableFrom(Character.class)) {
    		return new CharacterConverter();
    	}else if(inputClass.isAssignableFrom(Double.class)) {
    		return new DoubleConverter();
    	}else if(inputClass.isAssignableFrom(Float.class)) {
    		return new FloatConverter();
    	}else if(inputClass.isAssignableFrom(Integer.class)) {
    		return new IntegerConverter();
    	}else if(inputClass.isAssignableFrom(Long.class)) {
    		return new LongConverter();
    	}else if(inputClass.isAssignableFrom(Short.class)) {
    		return new ShortConverter();
    	}else if(inputClass.isAssignableFrom(Class.class)) {
    		return new ClassConverter();
    	}else if(inputClass.isAssignableFrom(File.class)) {
    		return new FileConverter();
    	}else if(inputClass.isAssignableFrom(URL.class)) {
    		return new URLConverter();
    	}
        
    	return new StringConverter();
    }
    
	public static <T> String convertToString(Field field, T obj) throws ConversionException {
		if (obj == null) {
			return null;
		}

		List<String> formats = new ArrayList<String>();
		String format = null;
		Locale locale = null;
		TimeZone timezone = null;
		
		if(field.isAnnotationPresent(EDIElementFormats.class)) {
			EDIElementFormats formatsAnnotation = field.getAnnotation(EDIElementFormats.class);
			EDIElementFormat[] elementFormats = formatsAnnotation.value();
			for (int i = 0; i < elementFormats.length; i++) {
				if (elementFormats[i].value() != null) {
					formats.add(elementFormats[i].value());
				}
			}
		}
		if(field.isAnnotationPresent(EDIElementFormat.class)) {
			EDIElementFormat formatAnnotation = field.getAnnotation(EDIElementFormat.class);
			format = formatAnnotation.value();
			if (format != null) {
				formats.add(format);
			}
		}
		
		if(field.isAnnotationPresent(EDILocale.class)) {
			EDILocale localeAnnotation = field.getAnnotation(EDILocale.class);
			locale = new Locale(localeAnnotation.language(), localeAnnotation.region());
		}
		else {	
			locale = Locale.getDefault();
		}
		 
		if(field.isAnnotationPresent(EDITimezone.class)) {
			EDITimezone timezoneAnnotation = field.getAnnotation(EDITimezone.class);
			timezone = TimeZone.getTimeZone(timezoneAnnotation.timezone());
		}
		else {
			timezone = TimeZone.getDefault();
		}
		
		try {
			//Converter converter = (Converter) ConvertUtils.convert(obj.getClass(), String.class);
			Converter converter = getConverterType(obj.getClass());
			
			if(converter instanceof NumberConverter) {
				if(obj instanceof Number && StringUtils.isNotBlank(format)) {
					NumberFormat numberFormat = new DecimalFormat(format);
					return numberFormat.format(obj);
				}
			}
			if(converter instanceof DateConverter) {
				return (String) new  StringLocaleConverter(locale,format).convert(String.class, obj,format);
				//return (String) new DateLocaleConverter(locale,format).convert(String.class, obj,format);
				//return ((DateConverter) converter).convert((Date)obj, locale, timezone, format);
			}
			return (String)converter.convert(String.class,obj);			
		} 
		catch(Exception e) {
			e.printStackTrace();
			throw new ConversionException("Exception converting object.", e);
		}

	}
	
	
	@SuppressWarnings("unchecked")
	public static <T> T convertFromString(Class<T> targetType, Field field, String val) throws ConversionException, ClassNotFoundException {
		if (val == null) {
			return null;
		}

		List<String> formats = new ArrayList<String>();
		Locale locale = null;
		TimeZone timezone = null;
		
		if(field.isAnnotationPresent(EDIElementFormats.class)) {
			EDIElementFormats formatsAnnotation = field.getAnnotation(EDIElementFormats.class);
			EDIElementFormat[] elementFormats = formatsAnnotation.value();
			for (int i = 0; i < elementFormats.length; i++) {
				if (elementFormats[i].value() != null) {
					formats.add(elementFormats[i].value());
				}
			}
		}
		
		if(field.isAnnotationPresent(EDIElementFormat.class)) {
			EDIElementFormat formatAnnotation = field.getAnnotation(EDIElementFormat.class);
			formats.add(formatAnnotation.value());
		}
		
		if(field.isAnnotationPresent(EDILocale.class)) {
			EDILocale localeAnnotation = field.getAnnotation(EDILocale.class);
			locale = new Locale(localeAnnotation.language(), localeAnnotation.region());
		}
		else {	
			locale = Locale.getDefault();
		}
		 
		if(field.isAnnotationPresent(EDITimezone.class)) {
			EDITimezone timezoneAnnotation = field.getAnnotation(EDITimezone.class);
			timezone = TimeZone.getTimeZone(timezoneAnnotation.timezone());
		}
		else {
			timezone = TimeZone.getDefault();
		}
		
		//Converter converter = Converters.getConverter(String.class, targetType);
		//Converter converter = (Converter) ConvertUtils.lookup(targetType);
		Converter converter = getConverterType(targetType);

		if (formats.size() > 0) {
			// Only supports one format pattern for now
			String format = formats.get(0);
			if(converter instanceof NumberConverter) {
				if(Number.class.isAssignableFrom(field.getType())) {
					Number number;
					if(StringUtils.isNotBlank(format)) {
						NumberFormat numberFormat = new DecimalFormat(format);
						try {
							number = numberFormat.parse(val);
							return (T)converter.convert(targetType,number);
						} catch (ParseException e) {
							throw new ConversionException("Exception converting ["+val+"] with format: "+format+" on class: "+targetType);
						}
					}
				
				}
			}
			if(converter instanceof DateConverter) {
				return  (T) new DateLocaleConverter(locale,format).convert(targetType, val,formats.get(0));
				//return (T)((StringToDate) converter).convert(val, locale, timezone, formats.get(0));
			}
		}
		//return (T)converter.convert(val);
		return (T)converter.convert(targetType,val);		
	}
	
	/**
	 * Validates an object against its JSR-303 validation annotations (STRICT MODE).
	 * This method delegates to EDIValidationUtil and throws on error.
	 * 
	 * @param <T> the type of object to validate
	 * @param object the object to validate
	 * @throws EDIMessageException if validation fails
	 */
	public static <T> void validateObject(T object) throws EDIMessageException {
		EDIValidationUtil.validate(object);
	}
	
	/**
	 * Validates an object and collects all errors (LENIENT MODE).
	 * Does NOT throw an exception. Returns validation result with all errors.
	 * 
	 * @param <T> the type of object to validate
	 * @param object the object to validate
	 * @return ValidationResult containing all validation errors
	 */
	public static <T> EDIValidationError.ValidationResult validateObjectLenient(T object) {
		return EDIValidationUtil.validateAndCollectErrors(object);
	}
	
	/**
	 * Validates an object and collects all errors with segment information (LENIENT MODE).
	 * 
	 * @param <T> the type of object to validate
	 * @param object the object to validate
	 * @param segmentTag the EDI segment tag
	 * @param lineNumber the line number in the EDI message
	 * @return ValidationResult containing all validation errors with segment information
	 */
	public static <T> EDIValidationError.ValidationResult validateObjectLenient(T object, String segmentTag, int lineNumber) {
		return EDIValidationUtil.validateAndCollectErrors(object, segmentTag, lineNumber);
	}
}
