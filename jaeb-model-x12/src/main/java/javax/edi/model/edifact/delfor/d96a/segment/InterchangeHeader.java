package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * UNB — Interchange Header.
 */
@EDISegment(tag = "UNB")
public class InterchangeHeader {
    @EDIElement(description = "Syntax identifier + version")
    @NotNull
    private String syntaxIdentifier;

    @EDIElement(description = "Interchange sender identification")
    @NotNull
    @Size(max = 35)
    private String interchangeSender;

    @EDIElement(description = "Interchange recipient identification")
    @NotNull
    @Size(max = 35)
    private String interchangeRecipient;

    @EDIElement(description = "Date/time of preparation")
    @NotNull
    private String dateTimeOfPreparation;

    @EDIElement(description = "Interchange control reference")
    @NotNull
    @Size(max = 14)
    private String interchangeControlReference;

    public String getSyntaxIdentifier() { return syntaxIdentifier; }
    public void setSyntaxIdentifier(String v) { this.syntaxIdentifier = v; }
    public String getInterchangeSender() { return interchangeSender; }
    public void setInterchangeSender(String v) { this.interchangeSender = v; }
    public String getInterchangeRecipient() { return interchangeRecipient; }
    public void setInterchangeRecipient(String v) { this.interchangeRecipient = v; }
    public String getDateTimeOfPreparation() { return dateTimeOfPreparation; }
    public void setDateTimeOfPreparation(String v) { this.dateTimeOfPreparation = v; }
    public String getInterchangeControlReference() { return interchangeControlReference; }
    public void setInterchangeControlReference(String v) { this.interchangeControlReference = v; }
}