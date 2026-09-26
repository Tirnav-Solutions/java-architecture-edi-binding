package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDISegmentGroup;
import javax.validation.constraints.NotNull;

@EDISegmentGroup
public class Trailer {

	private TotalWeightAndCharges totalWeightAndCharges;
	
	@NotNull
	private TransactionSetTrailer transactionSetTrailer;

	public TotalWeightAndCharges getTotalWeightAndCharges() {
		return totalWeightAndCharges;
	}

	public void setTotalWeightAndCharges(TotalWeightAndCharges totalWeightAndCharges) {
		this.totalWeightAndCharges = totalWeightAndCharges;
	}

	public TransactionSetTrailer getTransactionSetTrailer() {
		return transactionSetTrailer;
	}

	public void setTransactionSetTrailer(TransactionSetTrailer transactionSetTrailer) {
		this.transactionSetTrailer = transactionSetTrailer;
	}

	@Override
	public String toString() {
		return "Trailer [totalWeightAndCharges=" + totalWeightAndCharges
				+ ", transactionSetTrailer=" + transactionSetTrailer + "]";
	}
}
