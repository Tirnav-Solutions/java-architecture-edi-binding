package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** UNH — Message Header. */
@EDISegment(tag = "UNH")
public class MessageHeader {
    @EDIElement(description = "Message reference number")
    @NotNull
    @Size(max = 14)
    private String messageReferenceNumber;

    @EDIElement(description = "Message identifier (type:version:release:agency)")
    @NotNull
    private String messageIdentifier;

    public String getMessageReferenceNumber() { return messageReferenceNumber; }
    public void setMessageReferenceNumber(String v) { this.messageReferenceNumber = v; }
    public String getMessageIdentifier() { return messageIdentifier; }
    public void setMessageIdentifier(String v) { this.messageIdentifier = v; }
}