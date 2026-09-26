package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * DTM — Date/Time/Period.
 *
 * <pre>
 *   C507  DATE/TIME/PERIOD  M
 *     2005  Date/time/period qualifier         M  an..3
 *     2380  Date/time/period                   C  an..35
 *     2379  Date/time/period format qualifier  C  an..3
 * </pre>
 */
@EDISegment(tag = "DTM")
public class DateTimePeriod {

    @EDIComponent
    @NotNull
    @Size(min = 1, max = 3)
    private String dateTimePeriodQualifier;

    @EDIComponent
    @Size(max = 35)
    private String dateTimePeriodValue;

    @EDIComponent
    @Size(max = 3)
    private String dateTimePeriodFormatQualifier;

    public String getDateTimePeriodQualifier() { return dateTimePeriodQualifier; }
    public void setDateTimePeriodQualifier(String v) { this.dateTimePeriodQualifier = v; }

    public String getDateTimePeriodValue() { return dateTimePeriodValue; }
    public void setDateTimePeriodValue(String v) { this.dateTimePeriodValue = v; }

    public String getDateTimePeriodFormatQualifier() { return dateTimePeriodFormatQualifier; }
    public void setDateTimePeriodFormatQualifier(String v) { this.dateTimePeriodFormatQualifier = v; }
}