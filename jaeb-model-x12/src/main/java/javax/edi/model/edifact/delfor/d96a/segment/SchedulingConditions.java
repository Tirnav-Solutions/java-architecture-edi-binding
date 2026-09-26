package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** SCC — Scheduling Conditions. */
@EDISegment(tag = "SCC")
public class SchedulingConditions {
    @EDIElement(description = "Delivery plan commitment level code (1=firm, 4=forecast)")
    @NotNull
    @Size(min = 1, max = 3)
    private String deliveryPlanStatusCode;

    @EDIElement(description = "Delivery requirement, coded")
    @Size(max = 3)
    private String deliveryRequirementCode;

    @EDIElement(description = "Pattern description (D=daily, W=weekly, M=monthly)")
    @Size(max = 3)
    private String patternDescription;

    public String getDeliveryPlanStatusCode() { return deliveryPlanStatusCode; }
    public void setDeliveryPlanStatusCode(String v) { this.deliveryPlanStatusCode = v; }
    public String getDeliveryRequirementCode() { return deliveryRequirementCode; }
    public void setDeliveryRequirementCode(String v) { this.deliveryRequirementCode = v; }
    public String getPatternDescription() { return patternDescription; }
    public void setPatternDescription(String v) { this.patternDescription = v; }
}