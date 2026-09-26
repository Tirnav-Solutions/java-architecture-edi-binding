package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * QTY — Quantity.
 *
 * <pre>
 *   C186  QUANTITY DETAILS  M
 *     6063  Quantity qualifier       M  an..3
 *     6060  Quantity                 M  n..15
 *     6411  Measure unit qualifier   C  an..3  (default=PCE)
 * </pre>
 */
@EDISegment(tag = "QTY")
public class Quantity {

    @EDIComponent
    @NotNull
    @Size(min = 1, max = 3)
    private String quantityQualifier;

    @EDIComponent
    @NotNull
    @Size(min = 1, max = 15)
    private String quantity;

    @EDIComponent
    @Size(max = 3)
    private String measureUnitQualifier;

    public String getQuantityQualifier() { return quantityQualifier; }
    public void setQuantityQualifier(String v) { this.quantityQualifier = v; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String v) { this.quantity = v; }

    public String getMeasureUnitQualifier() { return measureUnitQualifier; }
    public void setMeasureUnitQualifier(String v) { this.measureUnitQualifier = v; }
}