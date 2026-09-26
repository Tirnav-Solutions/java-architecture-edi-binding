package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@EDISegment(tag="LH1")
public class HazardousIdentificationInformation {

	@EDIElement(fieldName="LH101", dataElement="355")
	@NotNull
	@Size(min=2, max=2)
	private String unitOrBasisForMeasurementCode;

	@EDIElement(fieldName="LH102", dataElement="80")
	@NotNull
	@Size(min=1, max=7)
	private String ladingQuantity;

	@EDIElement(fieldName="LH103", dataElement="277")
	@Size(min=6, max=6)
	private String unNaIdentificationCode;

	@EDIElement(fieldName="LH105", dataElement="22")
	@Size(min=1, max=30)
	private String commodityCode;

	@EDIElement(fieldName="LH110", dataElement="254")
	@Size(min=1, max=3)
	private String packingGroupCode;

	public String getUnitOrBasisForMeasurementCode() {
		return unitOrBasisForMeasurementCode;
	}

	public void setUnitOrBasisForMeasurementCode(String unitOrBasisForMeasurementCode) {
		this.unitOrBasisForMeasurementCode = unitOrBasisForMeasurementCode;
	}

	public String getLadingQuantity() {
		return ladingQuantity;
	}

	public void setLadingQuantity(String ladingQuantity) {
		this.ladingQuantity = ladingQuantity;
	}

	public String getUnNaIdentificationCode() {
		return unNaIdentificationCode;
	}

	public void setUnNaIdentificationCode(String unNaIdentificationCode) {
		this.unNaIdentificationCode = unNaIdentificationCode;
	}

	public String getCommodityCode() {
		return commodityCode;
	}

	public void setCommodityCode(String commodityCode) {
		this.commodityCode = commodityCode;
	}

	public String getPackingGroupCode() {
		return packingGroupCode;
	}

	public void setPackingGroupCode(String packingGroupCode) {
		this.packingGroupCode = packingGroupCode;
	}
}
