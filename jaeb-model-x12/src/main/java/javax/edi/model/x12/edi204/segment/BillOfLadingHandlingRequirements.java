package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

@EDISegment(tag="AT5")
public class BillOfLadingHandlingRequirements {

	@EDIElement(fieldName="AT501", dataElement="152")
	@Size(min=2, max=3)
	private String specialHandlingCode;

	@EDIElement(fieldName="AT503", dataElement="153")
	@Size(min=2, max=30)
	private String specialHandlingDescription;

	public String getSpecialHandlingCode() {
		return specialHandlingCode;
	}

	public void setSpecialHandlingCode(String specialHandlingCode) {
		this.specialHandlingCode = specialHandlingCode;
	}

	public String getSpecialHandlingDescription() {
		return specialHandlingDescription;
	}

	public void setSpecialHandlingDescription(String specialHandlingDescription) {
		this.specialHandlingDescription = specialHandlingDescription;
	}
}
