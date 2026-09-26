package javax.edi.model.edifact.delfor.d96a.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

/** PAC — Package. */
@EDISegment(tag = "PAC")
public class PackageInfo {
    @EDIElement(description = "Number of packages")
    @Size(max = 8)
    private String numberOfPackages;

    @EDIElement(description = "Packaging details")
    private String packagingDetails;

    @EDIElement(description = "Package type identification")
    @Size(max = 35)
    private String packageType;

    public String getNumberOfPackages() { return numberOfPackages; }
    public void setNumberOfPackages(String v) { this.numberOfPackages = v; }
    public String getPackagingDetails() { return packagingDetails; }
    public void setPackagingDetails(String v) { this.packagingDetails = v; }
    public String getPackageType() { return packageType; }
    public void setPackageType(String v) { this.packageType = v; }
}