package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class HazardousIdentificationLoop {

	private HazardousIdentificationInformation hazardousIdentificationInformation;

	@EDICollectionType(HazardousClassificationInformation.class)
	@Size(max=4)
	private Collection<HazardousClassificationInformation> hazardousClassificationInformations;

	@EDICollectionType(HazardousMaterialShippingName.class)
	@Size(max=10)
	private Collection<HazardousMaterialShippingName> hazardousMaterialShippingNames;

	public HazardousIdentificationInformation getHazardousIdentificationInformation() {
		return hazardousIdentificationInformation;
	}

	public void setHazardousIdentificationInformation(HazardousIdentificationInformation hazardousIdentificationInformation) {
		this.hazardousIdentificationInformation = hazardousIdentificationInformation;
	}

	public Collection<HazardousClassificationInformation> getHazardousClassificationInformations() {
		return hazardousClassificationInformations;
	}

	public void setHazardousClassificationInformations(Collection<HazardousClassificationInformation> hazardousClassificationInformations) {
		this.hazardousClassificationInformations = hazardousClassificationInformations;
	}

	public Collection<HazardousMaterialShippingName> getHazardousMaterialShippingNames() {
		return hazardousMaterialShippingNames;
	}

	public void setHazardousMaterialShippingNames(Collection<HazardousMaterialShippingName> hazardousMaterialShippingNames) {
		this.hazardousMaterialShippingNames = hazardousMaterialShippingNames;
	}
}
