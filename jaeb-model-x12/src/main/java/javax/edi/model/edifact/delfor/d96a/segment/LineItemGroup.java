package javax.edi.model.edifact.delfor.d96a.segment;

import java.util.List;

import javax.edi.bind.annotations.EDISegmentGroup;

/**
 * LIN Line Item group for DELFOR D96A.
 *
 * <p>Groups a Line Item (LIN) segment with all its dependent child segments
 * that appear in the detail section of the DELFOR message.</p>
 *
 * <pre>
 *   LIN  Line Item (header of group)
 *   IMD  Item Description (repeating)
 *   LOC  Location Identification (repeating)
 *   DTM  Date/Time/Period (repeating — item-level dates)
 *   RFF  Reference (repeating — item-level references)
 *   QTY  Quantity (repeating — cumulative, received, etc.)
 *   SCC group (repeating — schedule lines with DTM+QTY)
 *   PAC  Package (repeating)
 * </pre>
 */
@EDISegmentGroup(header = "LIN")
public class LineItemGroup {

    private LineItem lineItem;                            // LIN
    private List<ItemDescription> itemDescriptions;       // IMD (repeating)
    private List<LocationIdentification> locations;       // LOC (repeating)
    private List<DateTimePeriod> dateTimePeriods;          // DTM (repeating)
    private List<Reference> references;                   // RFF (repeating)
    private List<Quantity> quantities;                     // QTY (repeating)
    private List<ScheduleDetail> scheduleDetails;         // SCC groups (repeating)
    private List<PackageInfo> packages;                   // PAC (repeating)

    public LineItem getLineItem() { return lineItem; }
    public void setLineItem(LineItem v) { this.lineItem = v; }

    public List<ItemDescription> getItemDescriptions() { return itemDescriptions; }
    public void setItemDescriptions(List<ItemDescription> v) { this.itemDescriptions = v; }

    public List<LocationIdentification> getLocations() { return locations; }
    public void setLocations(List<LocationIdentification> v) { this.locations = v; }

    public List<DateTimePeriod> getDateTimePeriods() { return dateTimePeriods; }
    public void setDateTimePeriods(List<DateTimePeriod> v) { this.dateTimePeriods = v; }

    public List<Reference> getReferences() { return references; }
    public void setReferences(List<Reference> v) { this.references = v; }

    public List<Quantity> getQuantities() { return quantities; }
    public void setQuantities(List<Quantity> v) { this.quantities = v; }

    public List<ScheduleDetail> getScheduleDetails() { return scheduleDetails; }
    public void setScheduleDetails(List<ScheduleDetail> v) { this.scheduleDetails = v; }

    public List<PackageInfo> getPackages() { return packages; }
    public void setPackages(List<PackageInfo> v) { this.packages = v; }
}
