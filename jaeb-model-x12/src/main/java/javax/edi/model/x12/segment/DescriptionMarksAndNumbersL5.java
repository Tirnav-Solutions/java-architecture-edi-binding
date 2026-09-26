package javax.edi.model.x12.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/**
 * L5 - Description, Marks and Numbers
 * 
 * Pos: 130
 * Max: 1
 * Detail - Optional
 * Loop: 0320
 * Elements: 4
 * 
 * User Option (Usage): Used
 * Purpose: To specify the line item in terms of description, quantity, packaging, and marks and numbers
 */
@EDISegment(tag="L5")
public class DescriptionMarksAndNumbersL5 {

	/**
	 * L502 - Lading Description
	 * 
	 * Element: 79
	 * Requirement: O (Optional)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/50
	 * Usage: Used
	 * 
	 * Description of an item as required for rating and billing purposes
	 */
	@EDIElement(fieldName="L502", dataElement="79", conditional=true)
	@Size(min=1, max=50)
	private String ladingDescription;

	/**
	 * L503 - Commodity Code
	 * 
	 * Element: 22
	 * Requirement: X (Conditional)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/30
	 * Usage: Used
	 * 
	 * Code describing a commodity or group of commodities
	 * 
	 * Syntax: P0304 - If either L503 or L504 is present, then the other is required.
	 */
	@EDIElement(fieldName="L503", dataElement="22", conditional=true)
	@Size(min=1, max=30)
	private String commodityCode;

	/**
	 * L504 - Commodity Code Qualifier
	 * 
	 * Element: 23
	 * Requirement: X (Conditional)
	 * Type: ID (Identifier)
	 * Min/Max: 1/1
	 * Usage: Used
	 * 
	 * Code identifying the commodity coding system used for Commodity Code
	 * 
	 * Valid Code Examples:
	 * - N: National Motor Freight Classification (NMFC)
	 * - T: Standard Transportation Commodity Code (STCC)
	 * 
	 * Syntax: P0304 - If either L503 or L504 is present, then the other is required.
	 */
	@EDIElement(fieldName="L504", dataElement="23", conditional=true)
	@Size(min=1, max=1)
	private String commodityCodeQualifier;

	/**
	 * L505 - Packaging Code
	 * 
	 * Element: 103
	 * Requirement: O (Optional)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 3/5
	 * Usage: Used
	 * 
	 * Code identifying the type of packaging; 
	 * Part 1: Packaging Form
	 * Part 2: Packaging Material; 
	 * if the Data Element is used, then Part 1 is always required
	 * All valid standard codes are used. (Total Codes: 203)
	 */
	@EDIElement(fieldName="L505", dataElement="103", conditional=true)
	@Size(min=3, max=5)
	private String packagingCode;

	// Syntax Rules:
	// P0304 - If either L503 or L504 is present, then the other is required.
	// C0706 - If L507 is present, then L506 is required.
	// P0809 - If either L508 or L509 is present, then the other is required.

	/**
	 * Gets the lading description
	 * 
	 * @return the lading description
	 */
	public String getLadingDescription() {
		return ladingDescription;
	}

	/**
	 * Sets the lading description
	 * 
	 * @param ladingDescription the lading description to set
	 */
	public void setLadingDescription(String ladingDescription) {
		this.ladingDescription = ladingDescription;
	}

	/**
	 * Gets the commodity code
	 * 
	 * @return the commodity code
	 */
	public String getCommodityCode() {
		return commodityCode;
	}

	/**
	 * Sets the commodity code
	 * 
	 * @param commodityCode the commodity code to set
	 */
	public void setCommodityCode(String commodityCode) {
		this.commodityCode = commodityCode;
	}

	/**
	 * Gets the commodity code qualifier
	 * 
	 * @return the commodity code qualifier (N for NMFC or T for STCC)
	 */
	public String getCommodityCodeQualifier() {
		return commodityCodeQualifier;
	}

	/**
	 * Sets the commodity code qualifier
	 * 
	 * @param commodityCodeQualifier the commodity code qualifier to set (N for NMFC or T for STCC)
	 */
	public void setCommodityCodeQualifier(String commodityCodeQualifier) {
		this.commodityCodeQualifier = commodityCodeQualifier;
	}

	/**
	 * Gets the packaging code
	 * 
	 * @return the packaging code
	 */
	public String getPackagingCode() {
		return packagingCode;
	}

	/**
	 * Sets the packaging code
	 * 
	 * @param packagingCode the packaging code to set
	 */
	public void setPackagingCode(String packagingCode) {
		this.packagingCode = packagingCode;
	}

	@Override
	public String toString() {
		return "DescriptionMarksAndNumbersL5 [ladingDescription=" + ladingDescription + ", commodityCode="
				+ commodityCode + ", commodityCodeQualifier=" + commodityCodeQualifier + ", packagingCode="
				+ packagingCode + "]";
	}
}
