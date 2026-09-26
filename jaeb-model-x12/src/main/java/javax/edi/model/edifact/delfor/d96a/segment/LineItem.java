package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/**
 * LIN — Line Item.
 *
 * <pre>
 *   1082  Line item number               C  an..6
 *   1229  Action request/notification    C  an..3
 *   C212  ITEM NUMBER IDENTIFICATION     C  (composite)
 *     7140  Item number                   C  an..35
 *     7143  Item number type, coded       C  an..3
 *     1131  Code list qualifier           C  an..3
 *     3055  Code list responsible agency  C  an..3
 * </pre>
 */
@EDISegment(tag = "LIN")
public class LineItem {

    @EDIElement(fieldName = "1082", dataElement = "1082", description = "Line item number")
    @Size(max = 6)
    private String lineItemNumber;

    @EDIElement(fieldName = "1229", dataElement = "1229", description = "Action request/notification, coded")
    @Size(max = 3)
    private String actionRequestCode;

    // --- C212 composite (split by component delimiter ':') ---
    @EDIComponent
    @Size(max = 35)
    private String itemNumber;

    @EDIComponent
    @Size(max = 3)
    private String itemNumberType;

    @EDIComponent
    @Size(max = 3)
    private String codeListQualifier;

    @EDIComponent
    @Size(max = 3)
    private String codeListResponsibleAgency;

    public String getLineItemNumber() { return lineItemNumber; }
    public void setLineItemNumber(String v) { this.lineItemNumber = v; }

    public String getActionRequestCode() { return actionRequestCode; }
    public void setActionRequestCode(String v) { this.actionRequestCode = v; }

    public String getItemNumber() { return itemNumber; }
    public void setItemNumber(String v) { this.itemNumber = v; }

    public String getItemNumberType() { return itemNumberType; }
    public void setItemNumberType(String v) { this.itemNumberType = v; }

    public String getCodeListQualifier() { return codeListQualifier; }
    public void setCodeListQualifier(String v) { this.codeListQualifier = v; }

    public String getCodeListResponsibleAgency() { return codeListResponsibleAgency; }
    public void setCodeListResponsibleAgency(String v) { this.codeListResponsibleAgency = v; }
}