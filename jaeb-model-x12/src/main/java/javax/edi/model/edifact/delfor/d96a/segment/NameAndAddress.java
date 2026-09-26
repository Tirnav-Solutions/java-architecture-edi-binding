package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * NAD — Name and Address.
 *
 * <pre>
 *   3035  Party qualifier                    M  an..3
 *   C082  PARTY IDENTIFICATION DETAILS       C  (composite)
 *     3039  Party id identification           M  an..35
 *     1131  Code list qualifier               C  an..3
 *     3055  Code list responsible agency      C  an..3
 *   C058  NAME AND ADDRESS                   C  an..35
 *   C080  PARTY NAME                         C  an..35
 *   ...
 * </pre>
 */
@EDISegment(tag = "NAD")
public class NameAndAddress {

    @EDIElement(fieldName = "3035", dataElement = "3035", description = "Party qualifier")
    @NotNull
    @Size(min = 2, max = 3)
    private String partyQualifier;

    // --- C082 composite (split by component delimiter ':') ---
    @EDIComponent
    @Size(max = 35)
    private String partyIdIdentification;

    @EDIComponent
    @Size(max = 3)
    private String codeListQualifier;

    @EDIComponent
    @Size(max = 3)
    private String codeListResponsibleAgency;

    // --- Remaining simple elements ---
    @EDIElement(description = "C058 - Name and address")
    @Size(max = 35)
    private String nameAndAddress;

    @EDIElement(description = "C080 - Party name")
    @Size(max = 35)
    private String partyName;

    public String getPartyQualifier() { return partyQualifier; }
    public void setPartyQualifier(String v) { this.partyQualifier = v; }

    public String getPartyIdIdentification() { return partyIdIdentification; }
    public void setPartyIdIdentification(String v) { this.partyIdIdentification = v; }

    public String getCodeListQualifier() { return codeListQualifier; }
    public void setCodeListQualifier(String v) { this.codeListQualifier = v; }

    public String getCodeListResponsibleAgency() { return codeListResponsibleAgency; }
    public void setCodeListResponsibleAgency(String v) { this.codeListResponsibleAgency = v; }

    public String getNameAndAddress() { return nameAndAddress; }
    public void setNameAndAddress(String v) { this.nameAndAddress = v; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String v) { this.partyName = v; }
}