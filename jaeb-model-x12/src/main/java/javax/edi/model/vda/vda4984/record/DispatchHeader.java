package javax.edi.model.vda.vda4984.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4984 Record Type 721 — Dispatch Header.
 * Header record for a dispatch/shipping advice.
 */
@VDARecord(type = "721")
public class DispatchHeader {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 2, description = "Version number") private String version;
    @VDAField(start = 5, length = 9, description = "Customer/buyer number") private String customerNumber;
    @VDAField(start = 14, length = 9, description = "Supplier number") private String supplierNumber;
    @VDAField(start = 23, length = 8, description = "Delivery note number") private String deliveryNoteNumber;
    @VDAField(start = 31, length = 6, description = "Delivery note date YYMMDD") private String deliveryNoteDate;
    @VDAField(start = 37, length = 6, description = "Shipping date YYMMDD") private String shippingDate;
    @VDAField(start = 43, length = 6, description = "Arrival date YYMMDD") private String arrivalDate;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String v) { this.customerNumber = v; }
    public String getSupplierNumber() { return supplierNumber; }
    public void setSupplierNumber(String v) { this.supplierNumber = v; }
    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String v) { this.deliveryNoteNumber = v; }
    public String getDeliveryNoteDate() { return deliveryNoteDate; }
    public void setDeliveryNoteDate(String v) { this.deliveryNoteDate = v; }
    public String getShippingDate() { return shippingDate; }
    public void setShippingDate(String v) { this.shippingDate = v; }
    public String getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(String v) { this.arrivalDate = v; }
}
