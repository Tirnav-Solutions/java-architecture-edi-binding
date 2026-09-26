package javax.edi.model.x12.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * G61 - Contact Information
 * 
 * Purpose: To identify a person or office to whom communications should be directed
 * 
 * User Option (Usage): Used
 * 
 * Element Summary:
 */
@EDISegment(tag="G61")
public class ContactInformation {

	/**
	 * G6101 - Contact Function Code
	 * 
	 * Element: 366
	 * Requirement: M (Must use)
	 * Type: ID (Identifier)
	 * Min/Max: 2/2
	 * Usage: Must use
	 * 
	 * Code identifying the major duty or responsibility of the person or group named
	 * 
	 * Valid Code Examples:
	 * - AL: Alternate Contact
	 * - CN: General Contact
	 * - IC: Information Contact
	 * - OC: Order Contact
	 */
	@EDIElement(fieldName="G6101", dataElement="366")
	@NotNull
	@Size(min=2, max=2)
	private String contactFunctionCode;

	/**
	 * G6102 - Name
	 * 
	 * Element: 93
	 * Requirement: M (Must use)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/60
	 * Usage: Must use
	 * 
	 * Free-form name
	 */
	@EDIElement(fieldName="G6102", dataElement="93")
	@NotNull
	@Size(min=1, max=60)
	private String name;

	/**
	 * G6103 - Communication Number Qualifier
	 * 
	 * Element: 365
	 * Requirement: X (Conditional)
	 * Type: ID (Identifier)
	 * Min/Max: 2/2
	 * Usage: Used
	 * 
	 * Code identifying the type of communication number
	 * 
	 * Valid Code Examples:
	 * - EM: Electronic Mail
	 * - FX: Facsimile
	 * - TE: Telephone
	 */
	@EDIElement(fieldName="G6103", dataElement="365", conditional=true)
	@Size(min=2, max=2)
	private String communicationNumberQualifier;

	/**
	 * G6104 - Communication Number
	 * 
	 * Element: 364
	 * Requirement: X (Conditional)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/80
	 * Usage: Used
	 * 
	 * Complete communications number including country or area code when applicable
	 */
	@EDIElement(fieldName="G6104", dataElement="364", conditional=true)
	@Size(min=1, max=80)
	private String communicationNumber;

	// Syntax Rules:
	// P0304 - If either G6103 or G6104 is present, then the other is required.

	/**
	 * Gets the contact function code
	 * 
	 * @return the contact function code (e.g., IC for Information Contact)
	 */
	public String getContactFunctionCode() {
		return contactFunctionCode;
	}

	/**
	 * Sets the contact function code
	 * 
	 * @param contactFunctionCode the contact function code to set (e.g., IC for Information Contact)
	 */
	public void setContactFunctionCode(String contactFunctionCode) {
		this.contactFunctionCode = contactFunctionCode;
	}

	/**
	 * Gets the name
	 * 
	 * @return the contact person or office name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name
	 * 
	 * @param name the contact person or office name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the communication number qualifier code
	 * 
	 * @return the communication number qualifier (e.g., TE for Telephone)
	 */
	public String getCommunicationNumberQualifier() {
		return communicationNumberQualifier;
	}

	/**
	 * Sets the communication number qualifier code
	 * 
	 * @param communicationNumberQualifier the communication number qualifier to set (e.g., TE for Telephone)
	 */
	public void setCommunicationNumberQualifier(String communicationNumberQualifier) {
		this.communicationNumberQualifier = communicationNumberQualifier;
	}

	/**
	 * Gets the communication number
	 * 
	 * @return the complete communications number including country or area code
	 */
	public String getCommunicationNumber() {
		return communicationNumber;
	}

	/**
	 * Sets the communication number
	 * 
	 * @param communicationNumber the complete communications number to set including country or area code
	 */
	public void setCommunicationNumber(String communicationNumber) {
		this.communicationNumber = communicationNumber;
	}

	@Override
	public String toString() {
		return "ContactInformation [contactFunctionCode=" + contactFunctionCode + ", name=" + name
				+ ", communicationNumberQualifier=" + communicationNumberQualifier + ", communicationNumber="
				+ communicationNumber + "]";
	}
}
