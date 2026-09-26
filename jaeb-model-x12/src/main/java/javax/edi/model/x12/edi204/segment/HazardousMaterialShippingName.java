package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

@EDISegment(tag="LH3")
public class HazardousMaterialShippingName {

	@EDIElement(fieldName="LH301", dataElement="224")
	@Size(min=1, max=25)
	private String hazardousMaterialShippingName;

	@EDIElement(fieldName="LH302", dataElement="984")
	@Size(min=1, max=1)
	private String hazardousMaterialShippingNameQualifier;

	public String getHazardousMaterialShippingName() {
		return hazardousMaterialShippingName;
	}

	public void setHazardousMaterialShippingName(String hazardousMaterialShippingName) {
		this.hazardousMaterialShippingName = hazardousMaterialShippingName;
	}

	public String getHazardousMaterialShippingNameQualifier() {
		return hazardousMaterialShippingNameQualifier;
	}

	public void setHazardousMaterialShippingNameQualifier(String hazardousMaterialShippingNameQualifier) {
		this.hazardousMaterialShippingNameQualifier = hazardousMaterialShippingNameQualifier;
	}
}
