package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * RFF — Reference.
 *
 * <pre>
 *   C506  REFERENCE  M
 *     1153  Reference qualifier               M  an..3
 *     1154  Reference number                  C  an..35
 *     1156  Line number                       C  an..6
 *     4000  Reference version number          C  an..35
 * </pre>
 */
@EDISegment(tag = "RFF")
public class Reference {

    @EDIComponent
    @NotNull
    @Size(min = 1, max = 3)
    private String referenceQualifier;

    @EDIComponent
    @Size(max = 35)
    private String referenceNumber;

    @EDIComponent
    @Size(max = 6)
    private String lineNumber;

    @EDIComponent
    @Size(max = 35)
    private String referenceVersionNumber;

    public String getReferenceQualifier() { return referenceQualifier; }
    public void setReferenceQualifier(String v) { this.referenceQualifier = v; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String v) { this.referenceNumber = v; }

    public String getLineNumber() { return lineNumber; }
    public void setLineNumber(String v) { this.lineNumber = v; }

    public String getReferenceVersionNumber() { return referenceVersionNumber; }
    public void setReferenceVersionNumber(String v) { this.referenceVersionNumber = v; }
}