package javax.edi.model.vda.vda4984.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4984 Record Type 729 — Dispatch Trailer.
 * Summary/totals for the dispatch advice message.
 */
@VDARecord(type = "729")
public class DispatchTrailer {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 9, description = "Customer/buyer number") private String customerNumber;
    @VDAField(start = 12, length = 9, description = "Supplier number") private String supplierNumber;
    @VDAField(start = 21, length = 8, description = "Delivery note number") private String deliveryNoteNumber;
    @VDAField(start = 29, length = 7, description = "Data record count") private String dataRecordCount;
    @VDAField(start = 36, length = 5, description = "Total packages") private String totalPackages;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String v) { this.customerNumber = v; }
    public String getSupplierNumber() { return supplierNumber; }
    public void setSupplierNumber(String v) { this.supplierNumber = v; }
    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String v) { this.deliveryNoteNumber = v; }
    public String getDataRecordCount() { return dataRecordCount; }
    public void setDataRecordCount(String v) { this.dataRecordCount = v; }
    public String getTotalPackages() { return totalPackages; }
    public void setTotalPackages(String v) { this.totalPackages = v; }
}
