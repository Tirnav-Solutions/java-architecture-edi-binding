package javax.edi.model.edifact.delfor.d96a.segment;

import java.util.List;

import javax.edi.bind.annotations.EDISegmentGroup;

/**
 * SCC Schedule Detail group.
 *
 * <p>Groups a Scheduling Conditions segment (SCC) with its dependent
 * DTM (date/time) and QTY (quantity) segments.</p>
 *
 * <pre>
 *   SCC  Scheduling Conditions (header of group)
 *   DTM  Date/time (repeating, e.g. start date, end date)
 *   QTY  Quantity (repeating)
 * </pre>
 */
@EDISegmentGroup(header = "SCC")
public class ScheduleDetail {

    private SchedulingConditions schedulingConditions;  // SCC
    private List<DateTimePeriod> dateTimePeriods;        // DTM (repeating)
    private List<Quantity> quantities;                   // QTY (repeating)

    public SchedulingConditions getSchedulingConditions() { return schedulingConditions; }
    public void setSchedulingConditions(SchedulingConditions v) { this.schedulingConditions = v; }

    public List<DateTimePeriod> getDateTimePeriods() { return dateTimePeriods; }
    public void setDateTimePeriods(List<DateTimePeriod> v) { this.dateTimePeriods = v; }

    public List<Quantity> getQuantities() { return quantities; }
    public void setQuantities(List<Quantity> v) { this.quantities = v; }
}
