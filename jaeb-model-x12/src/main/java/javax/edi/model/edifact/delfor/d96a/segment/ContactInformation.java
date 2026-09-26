package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** CTA — Contact Information. */
@EDISegment(tag = "CTA")
public class ContactInformation {
    @EDIElement(description = "Contact function, coded")
    @NotNull
    @Size(max = 3)
    private String contactFunctionCode;

    @EDIElement(description = "Department or employee details")
    @Size(max = 35)
    private String departmentOrEmployee;

    public String getContactFunctionCode() { return contactFunctionCode; }
    public void setContactFunctionCode(String v) { this.contactFunctionCode = v; }
    public String getDepartmentOrEmployee() { return departmentOrEmployee; }
    public void setDepartmentOrEmployee(String v) { this.departmentOrEmployee = v; }
}