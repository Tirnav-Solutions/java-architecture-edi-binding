package javax.edi.model.edifact.delfor.d96a;

import java.util.ArrayList;
import java.util.List;

import javax.edi.bind.annotations.EDIFACTMessage;
import javax.edi.model.edifact.delfor.d96a.segment.*;

/**
 * EDIFACT DELFOR D96A  Delivery Forecast / Delivery Schedule.
 *
 * <p>Used in automotive and manufacturing industries to communicate
 * future delivery requirements from buyer to supplier.
 * This is the 1996A directory version, commonly used in European
 * automotive (VW, BMW, Daimler, Bosch, Continental, ZF, etc.).</p>
 *
 * <p>Hierarchical message structure:</p>
 * <pre>
 *   UNB  Interchange Header
 *   UNH  Message Header
 *   BGM  Beginning of Message (241 = delivery schedule)
 *   DTM  Date/Time/Period (message-level dates)
 *   RFF  Reference (schedule number, contract ref)
 *   NAD  Name and Address (SE, BY — header level)
 *   CTA  Contact Information
 *   UNS  Section Control (D=detail)
 *   LIN group (repeating):
 *     LIN  Line Item
 *     IMD  Item Description
 *     LOC  Location (delivery point, dock)
 *     DTM  Date/Time (item-level)
 *     RFF  Reference (item-level)
 *     QTY  Quantity (cumulative, received)
 *     SCC group (repeating):
 *       SCC  Scheduling Conditions
 *       DTM  Date/Time (schedule dates)
 *       QTY  Quantity (scheduled)
 *     PAC  Package
 *   UNS  Section Control (S=summary)
 *   UNT  Message Trailer
 *   UNZ  Interchange Trailer
 * </pre>
 */
@EDIFACTMessage(type = "DELFOR", version = "D96A")
public class DelforD96A {

    private InterchangeHeader interchangeHeader;            // UNB
    private MessageHeader messageHeader;                    // UNH
    private BeginningOfMessage beginningOfMessage;          // BGM
    private List<DateTimePeriod> dateTimePeriods;            // DTM (header-level, repeating)
    private List<Reference> references;                     // RFF (header-level, repeating)
    private List<NameAndAddress> nameAndAddresses;          // NAD (header-level, repeating)
    private List<ContactInformation> contacts;              // CTA (repeating)
    private List<SectionControl> sectionControls;           // UNS (repeating)
    private List<LineItemGroup> lineItemGroups;              // LIN groups (repeating)

    public InterchangeHeader getInterchangeHeader() { return interchangeHeader; }
    public void setInterchangeHeader(InterchangeHeader v) { this.interchangeHeader = v; }

    public MessageHeader getMessageHeader() { return messageHeader; }
    public void setMessageHeader(MessageHeader v) { this.messageHeader = v; }

    public BeginningOfMessage getBeginningOfMessage() { return beginningOfMessage; }
    public void setBeginningOfMessage(BeginningOfMessage v) { this.beginningOfMessage = v; }

    public List<DateTimePeriod> getDateTimePeriods() { return dateTimePeriods; }
    public void setDateTimePeriods(List<DateTimePeriod> v) { this.dateTimePeriods = v; }

    public List<Reference> getReferences() { return references; }
    public void setReferences(List<Reference> v) { this.references = v; }

    public List<NameAndAddress> getNameAndAddresses() { return nameAndAddresses; }
    public void setNameAndAddresses(List<NameAndAddress> v) { this.nameAndAddresses = v; }

    public List<ContactInformation> getContacts() { return contacts; }
    public void setContacts(List<ContactInformation> v) { this.contacts = v; }

    public List<SectionControl> getSectionControls() { return sectionControls; }
    public void setSectionControls(List<SectionControl> v) { this.sectionControls = v; }

    public List<LineItemGroup> getLineItemGroups() { return lineItemGroups; }
    public void setLineItemGroups(List<LineItemGroup> v) { this.lineItemGroups = v; }

    // ========================================================================
    // Convenience accessors (backward compatibility)
    // ========================================================================

    /** @return all LIN segments extracted from lineItemGroups, or null if none */
    public List<LineItem> getLineItems() {
        if (lineItemGroups == null) return null;
        List<LineItem> items = new ArrayList<>();
        for (LineItemGroup g : lineItemGroups) {
            if (g.getLineItem() != null) items.add(g.getLineItem());
        }
        return items.isEmpty() ? null : items;
    }

    /** @return all QTY segments from all lineItemGroups (item-level + schedule quantities), or null if none */
    public List<Quantity> getQuantities() {
        if (lineItemGroups == null) return null;
        List<Quantity> all = new ArrayList<>();
        for (LineItemGroup g : lineItemGroups) {
            if (g.getQuantities() != null) all.addAll(g.getQuantities());
            if (g.getScheduleDetails() != null) {
                for (ScheduleDetail sd : g.getScheduleDetails()) {
                    if (sd.getQuantities() != null) all.addAll(sd.getQuantities());
                }
            }
        }
        return all.isEmpty() ? null : all;
    }

    /** @return all SCC segments from all lineItemGroups, or null if none */
    public List<SchedulingConditions> getSchedulingConditions() {
        if (lineItemGroups == null) return null;
        List<SchedulingConditions> all = new ArrayList<>();
        for (LineItemGroup g : lineItemGroups) {
            if (g.getScheduleDetails() != null) {
                for (ScheduleDetail sd : g.getScheduleDetails()) {
                    if (sd.getSchedulingConditions() != null) all.add(sd.getSchedulingConditions());
                }
            }
        }
        return all.isEmpty() ? null : all;
    }

    /** @return all IMD segments from all lineItemGroups, or null if none */
    public List<ItemDescription> getItemDescriptions() {
        if (lineItemGroups == null) return null;
        List<ItemDescription> all = new ArrayList<>();
        for (LineItemGroup g : lineItemGroups) {
            if (g.getItemDescriptions() != null) all.addAll(g.getItemDescriptions());
        }
        return all.isEmpty() ? null : all;
    }

    /** @return all LOC segments from all lineItemGroups, or null if none */
    public List<LocationIdentification> getLocations() {
        if (lineItemGroups == null) return null;
        List<LocationIdentification> all = new ArrayList<>();
        for (LineItemGroup g : lineItemGroups) {
            if (g.getLocations() != null) all.addAll(g.getLocations());
        }
        return all.isEmpty() ? null : all;
    }

    /** @return all PAC segments from all lineItemGroups, or null if none */
    public List<PackageInfo> getPackages() {
        if (lineItemGroups == null) return null;
        List<PackageInfo> all = new ArrayList<>();
        for (LineItemGroup g : lineItemGroups) {
            if (g.getPackages() != null) all.addAll(g.getPackages());
        }
        return all.isEmpty() ? null : all;
    }
}