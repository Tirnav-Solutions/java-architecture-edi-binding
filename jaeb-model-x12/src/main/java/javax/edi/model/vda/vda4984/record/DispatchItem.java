package javax.edi.model.vda.vda4984.record;

import javax.edi.bind.annotations.VDAField;
import javax.edi.bind.annotations.VDARecord;

/**
 * VDA 4984 Record Type 722 — Item Record.
 * Line-item details for dispatched goods.
 */
@VDARecord(type = "722")
public class DispatchItem {
    @VDAField(start = 0, length = 3, description = "Record type", required = true) private String recordType;
    @VDAField(start = 3, length = 22, description = "Buyer's part number") private String buyerPartNumber;
    @VDAField(start = 25, length = 22, description = "Supplier's part number") private String supplierPartNumber;
    @VDAField(start = 47, length = 12, description = "Dispatched quantity") private String dispatchedQuantity;
    @VDAField(start = 59, length = 3, description = "Unit of measure") private String unitOfMeasure;
    @VDAField(start = 62, length = 6, description = "Delivery point / unloading point") private String deliveryPoint;
    @VDAField(start = 68, length = 10, description = "Order/call-off number") private String orderNumber;
    @VDAField(start = 78, length = 12, description = "Cumulative quantity") private String cumulativeQuantity;

    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { this.recordType = v; }
    public String getBuyerPartNumber() { return buyerPartNumber; }
    public void setBuyerPartNumber(String v) { this.buyerPartNumber = v; }
    public String getSupplierPartNumber() { return supplierPartNumber; }
    public void setSupplierPartNumber(String v) { this.supplierPartNumber = v; }
    public String getDispatchedQuantity() { return dispatchedQuantity; }
    public void setDispatchedQuantity(String v) { this.dispatchedQuantity = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getDeliveryPoint() { return deliveryPoint; }
    public void setDeliveryPoint(String v) { this.deliveryPoint = v; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String v) { this.orderNumber = v; }
    public String getCumulativeQuantity() { return cumulativeQuantity; }
    public void setCumulativeQuantity(String v) { this.cumulativeQuantity = v; }
}
