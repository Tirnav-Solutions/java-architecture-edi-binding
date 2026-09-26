package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegment(tag="B2")
public class BeginningSegmentForShipmentInformation {

	@EDIElement(fieldName="B201", dataElement="375")
	@Size(min=2, max=2)
	private String tariffServiceCode;

	@EDIElement(fieldName="B202", dataElement="140")
	@NotNull
	@Size(min=2, max=4)
	private String standardCarrierAlphaCode;

	@EDIElement(fieldName="B203", dataElement="154")
	@Size(min=6, max=9)
	private String standardPointLocationCode;

	@EDIElement(fieldName="B204", dataElement="145")
	@NotNull
	@Size(min=1, max=30)
	private String shipmentIdentificationNumber;

	@EDIElement(fieldName="B206", dataElement="146")
	@NotNull
	@Size(min=2, max=2)
	private String shipmentMethodOfPayment;

	public String getTariffServiceCode() {
		return tariffServiceCode;
	}

	public void setTariffServiceCode(String tariffServiceCode) {
		this.tariffServiceCode = tariffServiceCode;
	}

	public String getStandardCarrierAlphaCode() {
		return standardCarrierAlphaCode;
	}

	public void setStandardCarrierAlphaCode(String standardCarrierAlphaCode) {
		this.standardCarrierAlphaCode = standardCarrierAlphaCode;
	}

	public String getStandardPointLocationCode() {
		return standardPointLocationCode;
	}

	public void setStandardPointLocationCode(String standardPointLocationCode) {
		this.standardPointLocationCode = standardPointLocationCode;
	}

	public String getShipmentIdentificationNumber() {
		return shipmentIdentificationNumber;
	}

	public void setShipmentIdentificationNumber(String shipmentIdentificationNumber) {
		this.shipmentIdentificationNumber = shipmentIdentificationNumber;
	}

	public String getShipmentMethodOfPayment() {
		return shipmentMethodOfPayment;
	}

	public void setShipmentMethodOfPayment(String shipmentMethodOfPayment) {
		this.shipmentMethodOfPayment = shipmentMethodOfPayment;
	}
}
