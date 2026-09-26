package javax.edi.model.edifact.delfor.d04a;

import java.util.List;

import javax.edi.bind.annotations.EDIFACTMessage;
import javax.edi.model.edifact.delfor.d96a.segment.*;

/**
 * EDIFACT DELFOR D04A — Delivery Forecast / Delivery Schedule.
 *
 * <p>Directory version 2004A. Structurally similar to D96A but adds
 * optional segments for enhanced logistics and inventory management.</p>
 */
@EDIFACTMessage(type = "DELFOR", version = "D04A")
public class DelforD04A {

    private InterchangeHeader interchangeHeader;     // UNB
    private MessageHeader messageHeader;             // UNH
    private BeginningOfMessage beginningOfMessage;   // BGM
    private List<DateTimePeriod> dateTimePeriods;     // DTM
    private List<NameAndAddress> nameAndAddresses;   // NAD
    private List<LineItem> lineItems;                // LIN
    private List<Quantity> quantities;               // QTY

    public InterchangeHeader getInterchangeHeader() { return interchangeHeader; }
    public void setInterchangeHeader(InterchangeHeader v) { this.interchangeHeader = v; }

    public MessageHeader getMessageHeader() { return messageHeader; }
    public void setMessageHeader(MessageHeader v) { this.messageHeader = v; }

    public BeginningOfMessage getBeginningOfMessage() { return beginningOfMessage; }
    public void setBeginningOfMessage(BeginningOfMessage v) { this.beginningOfMessage = v; }

    public List<DateTimePeriod> getDateTimePeriods() { return dateTimePeriods; }
    public void setDateTimePeriods(List<DateTimePeriod> v) { this.dateTimePeriods = v; }

    public List<NameAndAddress> getNameAndAddresses() { return nameAndAddresses; }
    public void setNameAndAddresses(List<NameAndAddress> v) { this.nameAndAddresses = v; }

    public List<LineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<LineItem> v) { this.lineItems = v; }

    public List<Quantity> getQuantities() { return quantities; }
    public void setQuantities(List<Quantity> v) { this.quantities = v; }
}
