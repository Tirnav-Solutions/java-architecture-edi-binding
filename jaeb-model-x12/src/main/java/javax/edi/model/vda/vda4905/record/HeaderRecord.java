package javax.edi.model.vda.vda4905.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4905 Record Type 511 — Header Record.
 * Contains buyer/supplier identification, transfer date, and schedule metadata.
 */
@VDARecord(type = "511")
public class HeaderRecord {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 2, description = "Version number") private String version;
    @VDAField(start = 5, length = 9, description = "Customer/buyer number") private String customerNumber;
    @VDAField(start = 14, length = 9, description = "Supplier number") private String supplierNumber;
    @VDAField(start = 23, length = 6, description = "Transfer date YYMMDD") private String transferDate;
    @VDAField(start = 29, length = 5, description = "Transfer/transmission number") private String transferNumber;
    @VDAField(start = 34, length = 4, description = "Schedule number") private String scheduleNumber;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String v) { this.customerNumber = v; }
    public String getSupplierNumber() { return supplierNumber; }
    public void setSupplierNumber(String v) { this.supplierNumber = v; }
    public String getTransferDate() { return transferDate; }
    public void setTransferDate(String v) { this.transferDate = v; }
    public String getTransferNumber() { return transferNumber; }
    public void setTransferNumber(String v) { this.transferNumber = v; }
    public String getScheduleNumber() { return scheduleNumber; }
    public void setScheduleNumber(String v) { this.scheduleNumber = v; }
}
