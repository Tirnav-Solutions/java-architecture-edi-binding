package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** LOC — Place/Location Identification. */
@EDISegment(tag = "LOC")
public class LocationIdentification {
    @EDIElement(description = "Location function qualifier")
    @NotNull
    @Size(min = 1, max = 3)
    private String locationQualifier;

    @EDIElement(description = "Location identification")
    @Size(max = 25)
    private String locationId;

    public String getLocationQualifier() { return locationQualifier; }
    public void setLocationQualifier(String v) { this.locationQualifier = v; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String v) { this.locationId = v; }
}