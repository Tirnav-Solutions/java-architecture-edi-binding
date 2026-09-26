package javax.edi.model.vda.vda4905.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4905 Record Type 519 — Trailer Record.
 * Contains totals and counts for the message.
 */
@VDARecord(type = "519")
public class TrailerRecord {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 9, description = "Customer/buyer number") private String customerNumber;
    @VDAField(start = 12, length = 9, description = "Supplier number") private String supplierNumber;
    @VDAField(start = 21, length = 6, description = "Transfer date YYMMDD") private String transferDate;
    @VDAField(start = 27, length = 7, description = "Data record count") private String dataRecordCount;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String v) { this.customerNumber = v; }
    public String getSupplierNumber() { return supplierNumber; }
    public void setSupplierNumber(String v) { this.supplierNumber = v; }
    public String getTransferDate() { return transferDate; }
    public void setTransferDate(String v) { this.transferDate = v; }
    public String getDataRecordCount() { return dataRecordCount; }
    public void setDataRecordCount(String v) { this.dataRecordCount = v; }
}
