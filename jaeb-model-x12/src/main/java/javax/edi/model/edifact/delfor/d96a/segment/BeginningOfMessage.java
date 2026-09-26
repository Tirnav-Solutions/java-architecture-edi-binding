package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/**
 * BGM — Beginning of Message.
 *
 * <pre>
 *   C002  DOCUMENT/MESSAGE NAME      C  (composite)
 *     1001  Document/message name, coded  C  an..3
 *     1131  Code list qualifier           C  an..3
 *     3055  Code list responsible agency  C  an..3
 *     1000  Document/message name         C  an..35
 *   1004  Document/message number    C  an..35
 *   1225  Message function, coded    C  an..3
 * </pre>
 */
@EDISegment(tag = "BGM")
public class BeginningOfMessage {

    // --- C002 composite (split by component delimiter ':') ---
    @EDIComponent
    @Size(max = 3)
    private String documentNameCode;

    @EDIComponent
    @Size(max = 3)
    private String codeListQualifier;

    @EDIComponent
    @Size(max = 3)
    private String codeListResponsibleAgency;

    @EDIComponent
    @Size(max = 35)
    private String documentName;

    // --- Simple elements (split by element delimiter '+') ---
    @EDIElement(fieldName = "1004", dataElement = "1004", description = "Document/message number")
    @Size(max = 35)
    private String documentNumber;

    @EDIElement(fieldName = "1225", dataElement = "1225", description = "Message function, coded")
    @Size(max = 3)
    private String messageFunctionCode;

    public String getDocumentNameCode() { return documentNameCode; }
    public void setDocumentNameCode(String v) { this.documentNameCode = v; }

    public String getCodeListQualifier() { return codeListQualifier; }
    public void setCodeListQualifier(String v) { this.codeListQualifier = v; }

    public String getCodeListResponsibleAgency() { return codeListResponsibleAgency; }
    public void setCodeListResponsibleAgency(String v) { this.codeListResponsibleAgency = v; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String v) { this.documentName = v; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String v) { this.documentNumber = v; }

    public String getMessageFunctionCode() { return messageFunctionCode; }
    public void setMessageFunctionCode(String v) { this.messageFunctionCode = v; }
}