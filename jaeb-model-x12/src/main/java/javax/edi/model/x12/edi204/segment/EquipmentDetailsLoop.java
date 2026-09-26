package javax.edi.model.x12.edi204.segment;

import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.model.x12.segment.EquipmentDetails;

@EDISegmentGroup
public class EquipmentDetailsLoop {

	private EquipmentDetails equipmentDetails;

	public EquipmentDetails getEquipmentDetails() {
		return equipmentDetails;
	}

	public void setEquipmentDetails(EquipmentDetails equipmentDetails) {
		this.equipmentDetails= equipmentDetails;
	}
	
}
