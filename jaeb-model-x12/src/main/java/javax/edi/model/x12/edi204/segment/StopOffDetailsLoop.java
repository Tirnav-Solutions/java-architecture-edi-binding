package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.model.x12.segment.BusinessInstructionsAndReferenceNumber;
import javax.edi.model.x12.segment.DateTimeReferenceG62;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class StopOffDetailsLoop {

	@NotNull
	private StopOffDetails stopOffDetails;

	@EDICollectionType(BusinessInstructionsAndReferenceNumber.class)
	@Size(max=50)
	private Collection<BusinessInstructionsAndReferenceNumber> businessInstructionsAndReferenceNumbers;

	@EDICollectionType(DateTimeReferenceG62.class)
	@Size(max=2)
	private Collection<DateTimeReferenceG62> dateTimeReferences;

	@EDICollectionType(NoteSpecialInstruction.class)
	@Size(max=20)
	private Collection<NoteSpecialInstruction> noteSpecialInstructions;

	@EDICollectionType(StopOffNameLoop.class)
	@Size(max=1)
	private Collection<StopOffNameLoop> stopOffNameLoops;

	@EDICollectionType(DescriptionMarksAndNumbersLoop.class)
	@Size(max=99)
	private Collection<DescriptionMarksAndNumbersLoop> descriptionMarksAndNumbersLoops;

	public StopOffDetails getStopOffDetails() {
		return stopOffDetails;
	}

	public void setStopOffDetails(StopOffDetails stopOffDetails) {
		this.stopOffDetails = stopOffDetails;
	}

	public Collection<BusinessInstructionsAndReferenceNumber> getBusinessInstructionsAndReferenceNumbers() {
		return businessInstructionsAndReferenceNumbers;
	}

	public void setBusinessInstructionsAndReferenceNumbers(Collection<BusinessInstructionsAndReferenceNumber> businessInstructionsAndReferenceNumbers) {
		this.businessInstructionsAndReferenceNumbers = businessInstructionsAndReferenceNumbers;
	}

	public Collection<DateTimeReferenceG62> getDateTimeReferences() {
		return dateTimeReferences;
	}

	public void setDateTimeReferences(Collection<DateTimeReferenceG62> dateTimeReferences) {
		this.dateTimeReferences = dateTimeReferences;
	}

	public Collection<NoteSpecialInstruction> getNoteSpecialInstructions() {
		return noteSpecialInstructions;
	}

	public void setNoteSpecialInstructions(Collection<NoteSpecialInstruction> noteSpecialInstructions) {
		this.noteSpecialInstructions = noteSpecialInstructions;
	}

	public Collection<StopOffNameLoop> getStopOffNameLoops() {
		return stopOffNameLoops;
	}

	public void setStopOffNameLoops(Collection<StopOffNameLoop> stopOffNameLoops) {
		this.stopOffNameLoops = stopOffNameLoops;
	}

	public Collection<DescriptionMarksAndNumbersLoop> getDescriptionMarksAndNumbersLoops() {
		return descriptionMarksAndNumbersLoops;
	}

	public void setDescriptionMarksAndNumbersLoops(Collection<DescriptionMarksAndNumbersLoop> descriptionMarksAndNumbersLoops) {
		this.descriptionMarksAndNumbersLoops = descriptionMarksAndNumbersLoops;
	}
}
