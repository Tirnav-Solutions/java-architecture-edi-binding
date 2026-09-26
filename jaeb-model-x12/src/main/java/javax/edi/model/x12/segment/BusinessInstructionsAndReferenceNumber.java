package javax.edi.model.x12.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/**
 * L11 - Business Instructions and Reference Number
 * 
 * Pos: 080
 * Max: 50
 * Heading - Optional
 * Loop: N/A
 * Elements: 2
 * User Option (Usage): Used
 * 
 * Purpose: To specify instructions in this business relationship or a reference number
 */
@EDISegment(tag="L11")
public class BusinessInstructionsAndReferenceNumber {

	/**
	 * L1101 - Reference Identification
	 * 
	 * Element: 127
	 * Requirement: X (Either L1101 or L1103 is required)
	 * Type: AN (Alphanumeric)
	 * Min/Max: 1/30
	 * Usage: Used
	 * 
	 * Reference information as defined for a particular Transaction Set or as specified by the
	 * Reference Identification Qualifier
	 */
	@EDIElement(fieldName="L1101", dataElement="127", conditional=true)
	@Size(min=1, max=30)
	private String referenceIdentification;

	/**
	 * L1102 - Reference Identification Qualifier
	 * 
	 * Element: 128
	 * Requirement: X (Either conditional on L1101)
	 * Type: ID (Identifier)
	 * Min/Max: 2/3
	 * Usage: Used
	 * 
	 * Code qualifying the Reference Identification
	 * 
	 * Valid Code Examples:
	 * - 19: Division Identifier
	 * - 3J: Office Number
	 * - 4B: Shipment Origin Code
	 * - BB: Authorization Number
	 * - BM: Bill of Lading Number
	 * - BN: Booking Number
	 * - CO: Customer Order Number
	 * - CR: Customer Reference Number
	 * - DO: Delivery Order Number
	 * - EQ: Equipment Number
	 * - P8: Pickup Reference Number
	 * - PO: Purchase Order Number
	 * - RE: Release Number
	 * - SN: Seal Number
	 * - SO: Shipper's Order (Invoice Number)
	 * - SP: Scan Line
	 * - TH: Transportation Account Code (TAC)
	 * - TT: Terminal Code
	 * - VN: Vendor Order Number
	 * - DOA: Department of Agriculture Acquisition Regulation (AGAR)
	 * - RSN: Reservation Number
	 */
	@EDIElement(fieldName="L1102", dataElement="128", conditional=true)
	@Size(min=2, max=3)
	private String referenceIdentificationQualifier;

	// Syntax Rules:
	// R0103: At least one of L1101 or L1103 is required.
	// P0102: If either L1101 or L1102 is present, then the other is required.

	/**
	 * Gets the reference identification value
	 * 
	 * @return the reference identification
	 */
	public String getReferenceIdentification() {
		return referenceIdentification;
	}

	/**
	 * Sets the reference identification value
	 * 
	 * @param referenceIdentification the reference identification to set
	 */
	public void setReferenceIdentification(String referenceIdentification) {
		this.referenceIdentification = referenceIdentification;
	}

	/**
	 * Gets the reference identification qualifier code
	 * 
	 * @return the reference identification qualifier
	 */
	public String getReferenceIdentificationQualifier() {
		return referenceIdentificationQualifier;
	}

	/**
	 * Sets the reference identification qualifier code
	 * 
	 * @param referenceIdentificationQualifier the reference identification qualifier to set
	 */
	public void setReferenceIdentificationQualifier(String referenceIdentificationQualifier) {
		this.referenceIdentificationQualifier = referenceIdentificationQualifier;
	}

	@Override
	public String toString() {
		return "BusinessInstructionsAndReferenceNumber [referenceIdentification=" + referenceIdentification
				+ ", referenceIdentificationQualifier=" + referenceIdentificationQualifier + "]";
	}
}
