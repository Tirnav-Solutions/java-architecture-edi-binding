package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@EDISegment(tag="S5")
public class StopOffDetails {

	@EDIElement(fieldName="S501", dataElement="165")
	@NotNull
	@Size(min=1, max=3)
	private String stopSequenceNumber;

	@EDIElement(fieldName="S502", dataElement="163")
	@NotNull
	@Size(min=2, max=2)
	private String stopReasonCode;

	@EDIElement(fieldName="S503", dataElement="81")
	private BigDecimal weight;

	@EDIElement(fieldName="S504", dataElement="188")
	@Size(min=1, max=1)
	private String weightUnitCode;

	@EDIElement(fieldName="S510", dataElement="154")
	@Size(min=6, max=9)
	private String standardPointLocationCode;

	public String getStopSequenceNumber() {
		return stopSequenceNumber;
	}

	public void setStopSequenceNumber(String stopSequenceNumber) {
		this.stopSequenceNumber = stopSequenceNumber;
	}

	public String getStopReasonCode() {
		return stopReasonCode;
	}

	public void setStopReasonCode(String stopReasonCode) {
		this.stopReasonCode = stopReasonCode;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public void setWeight(BigDecimal weight) {
		this.weight = weight;
	}

	public String getWeightUnitCode() {
		return weightUnitCode;
	}

	public void setWeightUnitCode(String weightUnitCode) {
		this.weightUnitCode = weightUnitCode;
	}

	public String getStandardPointLocationCode() {
		return standardPointLocationCode;
	}

	public void setStandardPointLocationCode(String standardPointLocationCode) {
		this.standardPointLocationCode = standardPointLocationCode;
	}
}
