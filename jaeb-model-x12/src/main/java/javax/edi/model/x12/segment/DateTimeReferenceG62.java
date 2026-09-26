package javax.edi.model.x12.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.elements.EDIElementFormat;
import javax.validation.constraints.Size;

/**
 * G62 - Date/Time Reference
 * 
 * Pos: 090
 * Max: 1
 * Heading - Optional
 * Loop: N/A
 * Elements: 5
 * User Option (Usage): Used
 * 
 * Purpose: To specify pertinent dates and times
 */
@EDISegment(tag="G62")
public class DateTimeReferenceG62 {

	/**
	 * G6201 - Date Qualifier
	 * 
	 * Element: 432
	 * Requirement: X (Either G6201 or G6203 is required)
	 * Type: ID (Identifier)
	 * Min/Max: 2/2
	 * Usage: Used
	 * 
	 * Code specifying type of date
	 * 
	 * Valid Code Examples:
	 * - 64: Must Respond By
	 */
	@EDIElement(fieldName="G6201", dataElement="432", conditional=true)
	@Size(min=2, max=2)
	private String dateQualifier;

	/**
	 * G6202 - Date
	 * 
	 * Element: 373
	 * Requirement: X (Conditional on G6201)
	 * Type: DT (Date)
	 * Min/Max: 8/8
	 * Usage: Used
	 * Format: CCYYMMDD
	 * 
	 * Date expressed as CCYYMMDD
	 */
	@EDIElement(fieldName="G6202", dataElement="373", conditional=true)
	@EDIElementFormat("yyyyMMdd")
	@Size(min=8, max=8)
	private String date;

	/**
	 * G6203 - Time Qualifier
	 * 
	 * Element: 176
	 * Requirement: X (Either G6201 or G6203 is required)
	 * Type: ID (Identifier)
	 * Min/Max: 1/2
	 * Usage: Used
	 * 
	 * Code specifying the reported time
	 * 
	 * Valid Code Examples:
	 * - 1: Must Respond By
	 */
	@EDIElement(fieldName="G6203", dataElement="176", conditional=true)
	@Size(min=1, max=2)
	private String timeQualifier;

	/**
	 * G6204 - Time
	 * 
	 * Element: 337
	 * Requirement: X (Conditional on G6203)
	 * Type: TM (Time)
	 * Min/Max: 4/8
	 * Usage: Used
	 * Format: HHMM, HHMMSS, HHMMSSD, or HHMMSSDD
	 * 
	 * Time expressed in 24-hour clock time as follows:
	 * - HHMM: hours (00-23), minutes (00-59)
	 * - HHMMSS: hours (00-23), minutes (00-59), integer seconds (00-59)
	 * - HHMMSSD: hours, minutes, seconds, tenths (0-9)
	 * - HHMMSSDD: hours, minutes, seconds, hundredths (00-99)
	 */
	@EDIElement(fieldName="G6204", dataElement="337", conditional=true)
	@Size(min=4, max=8)
	private String time;

	/**
	 * G6205 - Time Code
	 * 
	 * Element: 623
	 * Requirement: X (Optional)
	 * Type: ID (Identifier)
	 * Min/Max: 2/2
	 * Usage: Used
	 * 
	 * Code identifying the time. In accordance with International Standards Organization
	 * standard 8601, time can be specified by a + or - and an indication in hours in relation 
	 * to Universal Time Coordinate (UTC) time; since + is a restricted character, + and - are 
	 * substituted by P and M in the codes that follow
	 * 
	 * Valid Code Examples:
	 * - ET: Eastern Time
	 */
	@EDIElement(fieldName="G6205", dataElement="623", conditional=true)
	@Size(min=2, max=2)
	private String timeCode;

	// Syntax Rules:
	// R0103: At least one of G6201 or G6203 is required.
	// P0102: If either G6201 or G6202 is present, then the other is required.
	// P0304: If either G6203 or G6204 is present, then the other is required.

	/**
	 * Gets the date qualifier code
	 * 
	 * @return the date qualifier
	 */
	public String getDateQualifier() {
		return dateQualifier;
	}

	/**
	 * Sets the date qualifier code
	 * 
	 * @param dateQualifier the date qualifier to set
	 */
	public void setDateQualifier(String dateQualifier) {
		this.dateQualifier = dateQualifier;
	}

	/**
	 * Gets the date value
	 * 
	 * @return the date in YYYYMMDD format
	 */
	public String getDate() {
		return date;
	}

	/**
	 * Sets the date value
	 * 
	 * @param date the date to set in YYYYMMDD format
	 */
	public void setDate(String date) {
		this.date = date;
	}

	/**
	 * Gets the time qualifier code
	 * 
	 * @return the time qualifier
	 */
	public String getTimeQualifier() {
		return timeQualifier;
	}

	/**
	 * Sets the time qualifier code
	 * 
	 * @param timeQualifier the time qualifier to set
	 */
	public void setTimeQualifier(String timeQualifier) {
		this.timeQualifier = timeQualifier;
	}

	/**
	 * Gets the time value
	 * 
	 * @return the time in HH:MM or HH:MM:SS format
	 */
	public String getTime() {
		return time;
	}

	/**
	 * Sets the time value
	 * 
	 * @param time the time to set in HH:MM or HH:MM:SS format
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * Gets the time code
	 * 
	 * @return the time code (e.g., ET for Eastern Time)
	 */
	public String getTimeCode() {
		return timeCode;
	}

	/**
	 * Sets the time code
	 * 
	 * @param timeCode the time code to set (e.g., ET for Eastern Time)
	 */
	public void setTimeCode(String timeCode) {
		this.timeCode = timeCode;
	}

	@Override
	public String toString() {
		return "DateTimeReferenceG62 [dateQualifier=" + dateQualifier + ", date=" + date + ", timeQualifier="
				+ timeQualifier + ", time=" + time + ", timeCode=" + timeCode + "]";
	}
}
