package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/** IMD — Item Description. */
@EDISegment(tag = "IMD")
public class ItemDescription {
    @EDIElement(description = "Item description type, coded")
    @Size(max = 3)
    private String descriptionFormatCode;

    @EDIElement(description = "Item characteristic, coded")
    @Size(max = 3)
    private String itemCharacteristic;

    @EDIElement(description = "Item description")
    @Size(max = 256)
    private String itemDescription;

    public String getDescriptionFormatCode() { return descriptionFormatCode; }
    public void setDescriptionFormatCode(String v) { this.descriptionFormatCode = v; }
    public String getItemCharacteristic() { return itemCharacteristic; }
    public void setItemCharacteristic(String v) { this.itemCharacteristic = v; }
    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String v) { this.itemDescription = v; }
}