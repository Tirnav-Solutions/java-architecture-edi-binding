package javax.edi.model.vda.vda4905.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4905 Record Type 513 — Delivery Call Record.
 * Contains scheduled delivery date, quantity, and call-off type (firm/planning).
 */
@VDARecord(type = "513")
public class DeliveryCallRecord {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 6, description = "Delivery date YYMMDD") private String deliveryDate;
    @VDAField(start = 9, length = 1, description = "Delivery time code") private String deliveryTimeCode;
    @VDAField(start = 10, length = 12, description = "Delivery quantity") private String deliveryQuantity;
    @VDAField(start = 22, length = 1, description = "Call-off type (F=firm, P=planning)") private String callOffType;
    @VDAField(start = 23, length = 10, description = "Delivery instruction number") private String deliveryInstruction;
    @VDAField(start = 33, length = 6, description = "Shipping date YYMMDD") private String shippingDate;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String v) { this.deliveryDate = v; }
    public String getDeliveryTimeCode() { return deliveryTimeCode; }
    public void setDeliveryTimeCode(String v) { this.deliveryTimeCode = v; }
    public String getDeliveryQuantity() { return deliveryQuantity; }
    public void setDeliveryQuantity(String v) { this.deliveryQuantity = v; }
    public String getCallOffType() { return callOffType; }
    public void setCallOffType(String v) { this.callOffType = v; }
    public String getDeliveryInstruction() { return deliveryInstruction; }
    public void setDeliveryInstruction(String v) { this.deliveryInstruction = v; }
    public String getShippingDate() { return shippingDate; }
    public void setShippingDate(String v) { this.shippingDate = v; }
}
