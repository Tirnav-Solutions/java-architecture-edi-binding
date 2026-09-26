package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegment(tag="SE")
public class TransactionSetTrailer {

	@EDIElement(fieldName="SE01", dataElement="96")
	@NotNull
	@Size(min=1, max=10)
	private String numberOfIncludedSegments;

	@EDIElement(fieldName="SE02", dataElement="329")
	@NotNull
	@Size(min=4, max=9)
	private String transactionSetControlNumber;

	public String getNumberOfIncludedSegments() {
		return numberOfIncludedSegments;
	}

	public void setNumberOfIncludedSegments(String numberOfIncludedSegments) {
		this.numberOfIncludedSegments = numberOfIncludedSegments;
	}

	public String getTransactionSetControlNumber() {
		return transactionSetControlNumber;
	}

	public void setTransactionSetControlNumber(String transactionSetControlNumber) {
		this.transactionSetControlNumber = transactionSetControlNumber;
	}
}
