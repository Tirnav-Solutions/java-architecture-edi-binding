package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;

@EDISegment(tag="MS3")
public class InterlineInformation {

	@EDIElement(fieldName="MS301", dataElement="140")
	@Size(min=2, max=4)
	private String standardCarrierAlphaCode;

	@EDIElement(fieldName="MS302", dataElement="133")
	@Size(min=1, max=2)
	private String routingSequenceCode;

	@EDIElement(fieldName="MS303", dataElement="19")
	@Size(min=2, max=30)
	private String cityName;

	@EDIElement(fieldName="MS304", dataElement="91")
	@Size(min=1, max=2)
	private String transportationMethodTypeCode;

	@EDIElement(fieldName="MS305", dataElement="156")
	@Size(min=2, max=2)
	private String stateOrProvinceCode;

	public String getStandardCarrierAlphaCode() {
		return standardCarrierAlphaCode;
	}

	public void setStandardCarrierAlphaCode(String standardCarrierAlphaCode) {
		this.standardCarrierAlphaCode = standardCarrierAlphaCode;
	}

	public String getRoutingSequenceCode() {
		return routingSequenceCode;
	}

	public void setRoutingSequenceCode(String routingSequenceCode) {
		this.routingSequenceCode = routingSequenceCode;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getTransportationMethodTypeCode() {
		return transportationMethodTypeCode;
	}

	public void setTransportationMethodTypeCode(String transportationMethodTypeCode) {
		this.transportationMethodTypeCode = transportationMethodTypeCode;
	}

	public String getStateOrProvinceCode() {
		return stateOrProvinceCode;
	}

	public void setStateOrProvinceCode(String stateOrProvinceCode) {
		this.stateOrProvinceCode = stateOrProvinceCode;
	}
}
