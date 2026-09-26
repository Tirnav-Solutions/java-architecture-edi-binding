package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.model.x12.segment.DescriptionMarksAndNumbersL5;
import javax.edi.model.x12.segment.ShipmentWeightPackagingQuantityData;
import javax.edi.model.x12.segment.ContactInformation;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class DescriptionMarksAndNumbersLoop {

	private DescriptionMarksAndNumbersL5 marksAndNumbers;

	private ShipmentWeightPackagingQuantityData shipmentWeightPackagingQuantityData;

	@EDICollectionType(ContactInformation.class)
	@Size(max=1)
	private Collection<ContactInformation> contactInformations;

	@EDICollectionType(HazardousIdentificationLoop.class)
	@Size(max=25)
	private Collection<HazardousIdentificationLoop> hazardousIdentificationLoops;

	public DescriptionMarksAndNumbersL5 getMarksAndNumbers() {
		return marksAndNumbers;
	}

	public void setMarksAndNumbers(DescriptionMarksAndNumbersL5 marksAndNumbers) {
		this.marksAndNumbers = marksAndNumbers;
	}

	public ShipmentWeightPackagingQuantityData getShipmentWeightPackagingQuantityData() {
		return shipmentWeightPackagingQuantityData;
	}

	public void setShipmentWeightPackagingQuantityData(ShipmentWeightPackagingQuantityData shipmentWeightPackagingQuantityData) {
		this.shipmentWeightPackagingQuantityData = shipmentWeightPackagingQuantityData;
	}

	public Collection<ContactInformation> getContactInformations() {
		return contactInformations;
	}

	public void setContactInformations(Collection<ContactInformation> contactInformations) {
		this.contactInformations = contactInformations;
	}

	public Collection<HazardousIdentificationLoop> getHazardousIdentificationLoops() {
		return hazardousIdentificationLoops;
	}

	public void setHazardousIdentificationLoops(Collection<HazardousIdentificationLoop> hazardousIdentificationLoops) {
		this.hazardousIdentificationLoops = hazardousIdentificationLoops;
	}
}
