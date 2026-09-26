package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegment(tag="NTE")
public class NoteSpecialInstruction {

	@EDIElement(fieldName="NTE01", dataElement="363")
	@Size(min=3, max=3)
	private String noteReferenceCode;

	@EDIElement(fieldName="NTE02", dataElement="352")
	@NotNull
	@Size(min=1, max=80)
	private String description;

	public String getNoteReferenceCode() {
		return noteReferenceCode;
	}

	public void setNoteReferenceCode(String noteReferenceCode) {
		this.noteReferenceCode = noteReferenceCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
