package javax.edi.model.x12.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * N7 - Equipment Details
 * 
 * Pos: 200
 * Max: 1
 * Heading - Optional
 * Loop: 0200
 * Elements: 6
 * 
 * User Option (Usage): Used
 * Purpose: To identify the equipment
 */
@EDISegment(tag="N7")
public class EquipmentDetails {

	/**
	 * N701 - Equipment Initial
	 * 
	 * Element: 206
	 * Requirement: M (Must use)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/4
	 * Usage: Used
	 * 
	 * Prefix or alphabetic part of an equipment unit's identifying number
	 */
	@EDIElement(fieldName="N701", dataElement="206")
	@NotNull
	@Size(min=1, max=4)
	private String equipmentInitial;

	/**
	 * N702 - Equipment Number
	 * 
	 * Element: 207
	 * Requirement: M (Must use)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/10
	 * Usage: Must use
	 * 
	 * Sequencing or serial part of an equipment unit's identifying number (pure numeric form for
	 * equipment number is preferred)
	 */
	@EDIElement(fieldName="N702", dataElement="207")
	@NotNull
	@Size(min=1, max=10)
	private String equipmentNumber;

	/**
	 * N711 - Equipment Description Code
	 * 
	 * Element: 40
	 * Requirement: O (Optional)
	 * Type: ID (Identifier)
	 * Min/Max: 2/2
	 * Usage: Used
	 * 
	 * Code identifying type of equipment used for shipment
	 * All valid standard codes are used. (Total Codes: 134)
	 */
	@EDIElement(fieldName="N711", dataElement="40", conditional=true)
	@Size(min=2, max=2)
	private String equipmentDescriptionCode;

	/**
	 * N712 - Standard Carrier Alpha Code
	 * 
	 * Element: 140
	 * Requirement: O (Optional)
	 * Type: ID (Identifier)
	 * Min/Max: 2/4
	 * Usage: Used
	 * 
	 * Standard Carrier Alpha Code
	 * 
	 * Semantics: N712 is the owner of the equipment.
	 */
	@EDIElement(fieldName="N712", dataElement="140", conditional=true)
	@Size(min=2, max=4)
	private String standardCarrierAlphaCode;

	/**
	 * N713 - Temperature Control
	 * 
	 * Element: 319
	 * Requirement: O (Optional)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 3/6
	 * Usage: Used
	 * 
	 * Free-form abbreviation of temperature range or flash-point temperature
	 */
	@EDIElement(fieldName="N713", dataElement="319", conditional=true)
	@Size(min=3, max=6)
	private String temperatureControl;

	/**
	 * N715 - Equipment Length
	 * 
	 * Element: 567
	 * Requirement: O (Optional)
	 * Type: N0 (Numeric - no decimal places)
	 * Min/Max: 4/5
	 * Usage: Used
	 * 
	 * Length (in feet and inches) of equipment ordered or used to transport shipment
	 * Format: FFFII where FFF is feet and II is inches; the range for II is 00 through 11
	 */
	@EDIElement(fieldName="N715", dataElement="567", conditional=true)
	@Size(min=4, max=5)
	private String equipmentLength;

	// Syntax Rules:
	// P0304 - If either N703 or N704 is present, then the other is required.
	// P0516 - If either N705 or N716 is present, then the other is required.
	// P0809 - If either N708 or N709 is present, then the other is required.
	//
	// Semantics:
	// N712 is the owner of the equipment.
	// N723 is the operator or carrier of the rights of the equipment

	/**
	 * Gets the equipment initial
	 * 
	 * @return the equipment initial (prefix or alphabetic part of equipment identifying number)
	 */
	public String getEquipmentInitial() {
		return equipmentInitial;
	}

	/**
	 * Sets the equipment initial
	 * 
	 * @param equipmentInitial the equipment initial to set (prefix or alphabetic part)
	 */
	public void setEquipmentInitial(String equipmentInitial) {
		this.equipmentInitial = equipmentInitial;
	}

	/**
	 * Gets the equipment number
	 * 
	 * @return the equipment number (sequencing or serial part of identifying number)
	 */
	public String getEquipmentNumber() {
		return equipmentNumber;
	}

	/**
	 * Sets the equipment number
	 * 
	 * @param equipmentNumber the equipment number to set (sequencing or serial part)
	 */
	public void setEquipmentNumber(String equipmentNumber) {
		this.equipmentNumber = equipmentNumber;
	}

	/**
	 * Gets the equipment description code
	 * 
	 * @return the equipment description code
	 */
	public String getEquipmentDescriptionCode() {
		return equipmentDescriptionCode;
	}

	/**
	 * Sets the equipment description code
	 * 
	 * @param equipmentDescriptionCode the equipment description code to set
	 */
	public void setEquipmentDescriptionCode(String equipmentDescriptionCode) {
		this.equipmentDescriptionCode = equipmentDescriptionCode;
	}

	/**
	 * Gets the standard carrier alpha code
	 * 
	 * @return the standard carrier alpha code (owner of the equipment)
	 */
	public String getStandardCarrierAlphaCode() {
		return standardCarrierAlphaCode;
	}

	/**
	 * Sets the standard carrier alpha code
	 * 
	 * @param standardCarrierAlphaCode the standard carrier alpha code to set (owner of the equipment)
	 */
	public void setStandardCarrierAlphaCode(String standardCarrierAlphaCode) {
		this.standardCarrierAlphaCode = standardCarrierAlphaCode;
	}

	/**
	 * Gets the temperature control
	 * 
	 * @return the temperature control abbreviation
	 */
	public String getTemperatureControl() {
		return temperatureControl;
	}

	/**
	 * Sets the temperature control
	 * 
	 * @param temperatureControl the temperature control abbreviation to set
	 */
	public void setTemperatureControl(String temperatureControl) {
		this.temperatureControl = temperatureControl;
	}

	/**
	 * Gets the equipment length
	 * 
	 * @return the equipment length in FFFII format (feet and inches)
	 */
	public String getEquipmentLength() {
		return equipmentLength;
	}

	/**
	 * Sets the equipment length
	 * 
	 * @param equipmentLength the equipment length to set in FFFII format (feet and inches)
	 */
	public void setEquipmentLength(String equipmentLength) {
		this.equipmentLength = equipmentLength;
	}

	@Override
	public String toString() {
		return "EquipmentDetails [equipmentInitial=" + equipmentInitial + ", equipmentNumber=" + equipmentNumber
				+ ", equipmentDescriptionCode=" + equipmentDescriptionCode + ", standardCarrierAlphaCode="
				+ standardCarrierAlphaCode + ", temperatureControl=" + temperatureControl + ", equipmentLength="
				+ equipmentLength + "]";
	}
}
