package javax.edi.model.vda.vda4984.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4984 Record Type 723 — Packaging Record.
 * Packaging/container details for the dispatch.
 */
@VDARecord(type = "723")
public class PackagingRecord {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 3, description = "Packaging type code (PLT, BOX, etc.)") private String packagingTypeCode;
    @VDAField(start = 6, length = 20, description = "Container/packaging ID") private String containerId;
    @VDAField(start = 26, length = 5, description = "Number of packages") private String numberOfPackages;
    @VDAField(start = 31, length = 12, description = "Gross weight") private String grossWeight;
    @VDAField(start = 43, length = 12, description = "Net weight") private String netWeight;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getPackagingTypeCode() { return packagingTypeCode; }
    public void setPackagingTypeCode(String v) { this.packagingTypeCode = v; }
    public String getContainerId() { return containerId; }
    public void setContainerId(String v) { this.containerId = v; }
    public String getNumberOfPackages() { return numberOfPackages; }
    public void setNumberOfPackages(String v) { this.numberOfPackages = v; }
    public String getGrossWeight() { return grossWeight; }
    public void setGrossWeight(String v) { this.grossWeight = v; }
    public String getNetWeight() { return netWeight; }
    public void setNetWeight(String v) { this.netWeight = v; }
}
