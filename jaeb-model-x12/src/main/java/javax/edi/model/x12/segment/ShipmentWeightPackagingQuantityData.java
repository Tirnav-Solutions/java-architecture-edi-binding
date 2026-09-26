package javax.edi.model.x12.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/**
 * AT8 - Shipment Weight, Packaging and Quantity Data
 * 
 * Pos: 135
 * Max: 1
 * Detail - Optional
 * Loop: 0320
 * Elements: 4
 * 
 * User Option (Usage): Used
 * Purpose: To specify shipment details in terms of weight, and quantity of handling units
 */
@EDISegment(tag="AT8")
public class ShipmentWeightPackagingQuantityData {

	/**
	 * AT801 - Weight Qualifier
	 * 
	 * Element: 187
	 * Requirement: X (Conditional)
	 * Type: ID (Identifier)
	 * Min/Max: 1/2
	 * Usage: Used
	 * 
	 * Code defining the type of weight
	 * 
	 * Valid Code Examples:
	 * - G: Gross Weight
	 * 
	 * Syntax: P010203 - If either AT801, AT802 or AT803 are present, then the others are required.
	 */
	@EDIElement(fieldName="AT801", dataElement="187", conditional=true)
	@Size(min=1, max=2)
	private String weightQualifier;

	/**
	 * AT802 - Weight Unit Code
	 * 
	 * Element: 188
	 * Requirement: X (Conditional)
	 * Type: ID (Identifier)
	 * Min/Max: 1/1
	 * Usage: Used
	 * 
	 * Code specifying the weight unit
	 * 
	 * Valid Code Examples:
	 * - K: Kilograms
	 * - L: Pounds
	 * 
	 * Syntax: P010203 - If either AT801, AT802 or AT803 are present, then the others are required.
	 */
	@EDIElement(fieldName="AT802", dataElement="188", conditional=true)
	@Size(min=1, max=1)
	private String weightUnitCode;

	/**
	 * AT803 - Weight
	 * 
	 * Element: 81
	 * Requirement: X (Conditional)
	 * Type: R (Numeric with implicit decimal)
	 * Min/Max: 1/10
	 * Usage: Used
	 * 
	 * Numeric value of weight
	 * 
	 * Syntax: P010203 - If either AT801, AT802 or AT803 are present, then the others are required.
	 */
	@EDIElement(fieldName="AT803", dataElement="81", conditional=true)
	@Size(min=1, max=10)
	private String weight;

	/**
	 * AT804 - Lading Quantity
	 * 
	 * Element: 80
	 * Requirement: O (Optional)
	 * Type: N0 (Numeric - no decimal places)
	 * Min/Max: 1/7
	 * Usage: Used
	 * 
	 * Number of units (pieces) of the lading commodity
	 * 
	 * Semantics: AT804 is the quantity of handling units that are not unitized (for example a carton). 
	 * When added to the quantity in AT805, it is the total quantity of handling units in the shipment.
	 */
	@EDIElement(fieldName="AT804", dataElement="80", conditional=true)
	@Size(min=1, max=7)
	private String ladingQuantity;

	// Syntax Rules:
	// P010203 - If either AT801, AT802 or AT803 are present, then the others are required.
	// P0607 - If either AT806 or AT807 is present, then the other is required.
	//
	// Semantics:
	// AT804 is the quantity of handling units that are not unitized (for example a carton). 
	// When added to the quantity in AT805, it is the total quantity of handling units in the shipment.
	//
	// AT805 is the quantity of handling units that are unitized (for example on a pallet or slip sheet). 
	// When added to the quantity in AT804 it is the total quantity of handling units for the shipment.

	/**
	 * Gets the weight qualifier code
	 * 
	 * @return the weight qualifier (e.g., G for Gross Weight)
	 */
	public String getWeightQualifier() {
		return weightQualifier;
	}

	/**
	 * Sets the weight qualifier code
	 * 
	 * @param weightQualifier the weight qualifier to set (e.g., G for Gross Weight)
	 */
	public void setWeightQualifier(String weightQualifier) {
		this.weightQualifier = weightQualifier;
	}

	/**
	 * Gets the weight unit code
	 * 
	 * @return the weight unit code (e.g., L for Pounds, K for Kilograms)
	 */
	public String getWeightUnitCode() {
		return weightUnitCode;
	}

	/**
	 * Sets the weight unit code
	 * 
	 * @param weightUnitCode the weight unit code to set (e.g., L for Pounds, K for Kilograms)
	 */
	public void setWeightUnitCode(String weightUnitCode) {
		this.weightUnitCode = weightUnitCode;
	}

	/**
	 * Gets the weight value
	 * 
	 * @return the numeric weight value
	 */
	public String getWeight() {
		return weight;
	}

	/**
	 * Sets the weight value
	 * 
	 * @param weight the numeric weight value to set
	 */
	public void setWeight(String weight) {
		this.weight = weight;
	}

	/**
	 * Gets the lading quantity
	 * 
	 * @return the number of units (pieces) of lading commodity (non-unitized handling units)
	 */
	public String getLadingQuantity() {
		return ladingQuantity;
	}

	/**
	 * Sets the lading quantity
	 * 
	 * @param ladingQuantity the number of units to set (non-unitized handling units)
	 */
	public void setLadingQuantity(String ladingQuantity) {
		this.ladingQuantity = ladingQuantity;
	}

	@Override
	public String toString() {
		return "ShipmentWeightPackagingQuantityData [weightQualifier=" + weightQualifier + ", weightUnitCode="
				+ weightUnitCode + ", weight=" + weight + ", ladingQuantity=" + ladingQuantity + "]";
	}
}
