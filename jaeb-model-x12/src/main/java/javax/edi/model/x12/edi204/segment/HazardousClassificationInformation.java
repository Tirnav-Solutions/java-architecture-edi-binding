package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

@EDISegment(tag="LH2")
public class HazardousClassificationInformation {

	@EDIElement(fieldName="LH201", dataElement="215")
	@Size(min=1, max=30)
	private String hazardousClassification;

	@EDIElement(fieldName="LH202", dataElement="983")
	@Size(min=1, max=1)
	private String hazardousClassQualifier;

	public String getHazardousClassification() {
		return hazardousClassification;
	}

	public void setHazardousClassification(String hazardousClassification) {
		this.hazardousClassification = hazardousClassification;
	}

	public String getHazardousClassQualifier() {
		return hazardousClassQualifier;
	}

	public void setHazardousClassQualifier(String hazardousClassQualifier) {
		this.hazardousClassQualifier = hazardousClassQualifier;
	}
}
