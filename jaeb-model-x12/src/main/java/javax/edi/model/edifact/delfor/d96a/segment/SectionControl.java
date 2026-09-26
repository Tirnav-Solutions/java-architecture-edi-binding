package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** UNS — Section Control. */
@EDISegment(tag = "UNS")
public class SectionControl {
    @EDIElement(description = "Section identification (D=detail, S=summary)")
    @NotNull
    @Size(min = 1, max = 1)
    private String sectionId;

    public String getSectionId() { return sectionId; }
    public void setSectionId(String v) { this.sectionId = v; }
}