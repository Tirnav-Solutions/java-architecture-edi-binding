package javax.edi.model.vda.vda4905.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4905 Record Type 512 — Material Record.
 * Identifies the part/material being scheduled.
 */
@VDARecord(type = "512")
public class MaterialRecord {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 22, description = "Buyer's part number") private String buyerPartNumber;
    @VDAField(start = 25, length = 22, description = "Supplier's part number") private String supplierPartNumber;
    @VDAField(start = 47, length = 6, description = "Delivery point / unloading point") private String deliveryPoint;
    @VDAField(start = 53, length = 10, description = "Previous delivery note number") private String previousDeliveryNote;
    @VDAField(start = 63, length = 6, description = "Previous delivery date YYMMDD") private String previousDeliveryDate;
    @VDAField(start = 69, length = 12, description = "Cumulative quantity received") private String cumulativeQuantity;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getBuyerPartNumber() { return buyerPartNumber; }
    public void setBuyerPartNumber(String v) { this.buyerPartNumber = v; }
    public String getSupplierPartNumber() { return supplierPartNumber; }
    public void setSupplierPartNumber(String v) { this.supplierPartNumber = v; }
    public String getDeliveryPoint() { return deliveryPoint; }
    public void setDeliveryPoint(String v) { this.deliveryPoint = v; }
    public String getPreviousDeliveryNote() { return previousDeliveryNote; }
    public void setPreviousDeliveryNote(String v) { this.previousDeliveryNote = v; }
    public String getPreviousDeliveryDate() { return previousDeliveryDate; }
    public void setPreviousDeliveryDate(String v) { this.previousDeliveryDate = v; }
    public String getCumulativeQuantity() { return cumulativeQuantity; }
    public void setCumulativeQuantity(String v) { this.cumulativeQuantity = v; }
}
