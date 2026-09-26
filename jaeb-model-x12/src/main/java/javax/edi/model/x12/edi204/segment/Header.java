package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.model.x12.segment.BusinessInstructionsAndReferenceNumber;
import javax.edi.model.x12.segment.DateTimeReference;
import javax.edi.model.x12.segment.DateTimeReferenceG62;
import javax.edi.model.x12.segment.ReferenceNumber;
import javax.edi.model.x12.segment.TransactionSetHeader;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class Header {

	@NotNull
	private TransactionSetHeader transactionSetHeader;
	
	@NotNull
	private BeginningSegmentForShipmentInformation beginningSegment;
	
	@NotNull
	private SetPurpose setPurpose;
	
	@EDICollectionType(BusinessInstructionsAndReferenceNumber.class)
	@Size(max=50)
	private Collection<BusinessInstructionsAndReferenceNumber> businessInstructionsAndReferenceNumber;
	
	@EDICollectionType(ReferenceNumber.class)
	@Size(max=50)
	private Collection<ReferenceNumber> referenceNumbers;
	
	@EDICollectionType(DateTimeReferenceG62.class)
	@Size(max=1)
	private Collection<DateTimeReferenceG62> dateTimeReferences;
	
	@EDICollectionType(InterlineInformation.class)
	@Size(max=1)
	private Collection<InterlineInformation> interlineInformations;
	
	@EDICollectionType(BillOfLadingHandlingRequirements.class)
	@Size(max=6)
	private Collection<BillOfLadingHandlingRequirements> billOfLadingHandlingRequirements;
	
	@EDICollectionType(NoteSpecialInstruction.class)
	@Size(max=10)
	private Collection<NoteSpecialInstruction> noteSpecialInstructions;
	
	@EDICollectionType(NameLoop.class)
	@Size(max=5)
	private Collection<NameLoop> nameLoops;
	
	@EDICollectionType(EquipmentDetailsLoop.class)
	@Size(max=10)
	private Collection<EquipmentDetailsLoop> equipmentDetailsLoops;

	/**
	 * @return the transactionSetHeader
	 */
	public TransactionSetHeader getTransactionSetHeader() {
		return transactionSetHeader;
	}

	/**
	 * @return the setPurpose
	 */
	public SetPurpose getSetPurpose() {
		return setPurpose;
	}

	/**
	 * @param transactionSetHeader the transactionSetHeader to set
	 */
	public void setTransactionSetHeader(TransactionSetHeader transactionSetHeader) {
		this.transactionSetHeader = transactionSetHeader;
	}

	/**
	 * @param setPurpose the setPurpose to set
	 */
	public void setSetPurpose(SetPurpose setPurpose) {
		this.setPurpose = setPurpose;
	}

	public BeginningSegmentForShipmentInformation getBeginningSegment() {
		return beginningSegment;
	}

	public void setBeginningSegment(BeginningSegmentForShipmentInformation beginningSegment) {
		this.beginningSegment = beginningSegment;
	}

	/**
	 * @return the businessInstructionsAndReferenceNumber
	 */
	public Collection<BusinessInstructionsAndReferenceNumber> getBusinessInstructionsAndReferenceNumber() {
		return businessInstructionsAndReferenceNumber;
	}

	/**
	 * @param businessInstructionsAndReferenceNumber the businessInstructionsAndReferenceNumber to set
	 */
	public void setBusinessInstructionsAndReferenceNumber(
			Collection<BusinessInstructionsAndReferenceNumber> businessInstructionsAndReferenceNumber) {
		this.businessInstructionsAndReferenceNumber = businessInstructionsAndReferenceNumber;
	}

	public Collection<ReferenceNumber> getReferenceNumbers() {
		return referenceNumbers;
	}

	public void setReferenceNumbers(Collection<ReferenceNumber> referenceNumbers) {
		this.referenceNumbers = referenceNumbers;
	}

	public Collection<DateTimeReferenceG62> getDateTimeReferences() {
		return dateTimeReferences;
	}

	public void setDateTimeReferences(Collection<DateTimeReferenceG62> dateTimeReferences) {
		this.dateTimeReferences = dateTimeReferences;
	}

	public Collection<InterlineInformation> getInterlineInformations() {
		return interlineInformations;
	}

	public void setInterlineInformations(Collection<InterlineInformation> interlineInformations) {
		this.interlineInformations = interlineInformations;
	}

	public Collection<BillOfLadingHandlingRequirements> getBillOfLadingHandlingRequirements() {
		return billOfLadingHandlingRequirements;
	}

	public void setBillOfLadingHandlingRequirements(Collection<BillOfLadingHandlingRequirements> billOfLadingHandlingRequirements) {
		this.billOfLadingHandlingRequirements = billOfLadingHandlingRequirements;
	}

	public Collection<NoteSpecialInstruction> getNoteSpecialInstructions() {
		return noteSpecialInstructions;
	}

	public void setNoteSpecialInstructions(Collection<NoteSpecialInstruction> noteSpecialInstructions) {
		this.noteSpecialInstructions = noteSpecialInstructions;
	}

	public Collection<NameLoop> getNameLoops() {
		return nameLoops;
	}

	public void setNameLoops(Collection<NameLoop> nameLoops) {
		this.nameLoops = nameLoops;
	}

	public Collection<EquipmentDetailsLoop> getEquipmentDetailsLoops() {
		return equipmentDetailsLoops;
	}

	public void setEquipmentDetailsLoops(Collection<EquipmentDetailsLoop> equipmentDetailsLoops) {
		this.equipmentDetailsLoops = equipmentDetailsLoops;
	}
}
