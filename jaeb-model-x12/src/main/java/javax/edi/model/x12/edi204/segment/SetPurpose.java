package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegment(tag="B2A")
public class SetPurpose {

	@EDIElement(fieldName="B2A01", dataElement="353")
	@NotNull
	@Size(min=2, max=2)
	private String transactionSetPurposeCode;

	@EDIElement(fieldName="B2A02", dataElement="346")
	@Size(min=2, max=2)
	private String applicationType;

	public String getTransactionSetPurposeCode() {
		return transactionSetPurposeCode;
	}

	public void setTransactionSetPurposeCode(String transactionSetPurposeCode) {
		this.transactionSetPurposeCode = transactionSetPurposeCode;
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(String applicationType) {
		this.applicationType = applicationType;
	}
}
