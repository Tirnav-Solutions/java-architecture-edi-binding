package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDISegment;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@EDISegment(tag="L3")
public class TotalWeightAndCharges {

	@EDIElement(fieldName="L301", dataElement="81")
	private BigDecimal weight;

	@EDIElement(fieldName="L302", dataElement="187")
	@Size(min=1, max=2)
	private String weightQualifier;

	@EDIElement(fieldName="L303", dataElement="60")
	private BigDecimal freightRate;

	@EDIElement(fieldName="L304", dataElement="122")
	@Size(min=2, max=2)
	private String rateValueQualifier;

	@EDIElement(fieldName="L305", dataElement="58")
	private BigDecimal charge;

	@EDIElement(fieldName="L306", dataElement="191")
	private BigDecimal advances;

	@EDIElement(fieldName="L311", dataElement="80")
	private String ladingQuantity;

	@EDIElement(fieldName="L312", dataElement="188")
	@Size(min=1, max=1)
	private String weightUnitCode;

	public BigDecimal getWeight() {
		return weight;
	}

	public void setWeight(BigDecimal weight) {
		this.weight = weight;
	}

	public String getWeightQualifier() {
		return weightQualifier;
	}

	public void setWeightQualifier(String weightQualifier) {
		this.weightQualifier = weightQualifier;
	}

	public BigDecimal getFreightRate() {
		return freightRate;
	}

	public void setFreightRate(BigDecimal freightRate) {
		this.freightRate = freightRate;
	}

	public String getRateValueQualifier() {
		return rateValueQualifier;
	}

	public void setRateValueQualifier(String rateValueQualifier) {
		this.rateValueQualifier = rateValueQualifier;
	}

	public BigDecimal getCharge() {
		return charge;
	}

	public void setCharge(BigDecimal charge) {
		this.charge = charge;
	}

	public BigDecimal getAdvances() {
		return advances;
	}

	public void setAdvances(BigDecimal advances) {
		this.advances = advances;
	}

	public String getLadingQuantity() {
		return ladingQuantity;
	}

	public void setLadingQuantity(String ladingQuantity) {
		this.ladingQuantity = ladingQuantity;
	}

	public String getWeightUnitCode() {
		return weightUnitCode;
	}

	public void setWeightUnitCode(String weightUnitCode) {
		this.weightUnitCode = weightUnitCode;
	}
}
