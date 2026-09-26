package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class Detail {

	@NotNull
	@Size(min=1)
	@EDICollectionType(StopOffDetailsLoop.class)
	private Collection<StopOffDetailsLoop> stopOffDetailsLoops;

	public Collection<StopOffDetailsLoop> getStopOffDetailsLoops() {
		return stopOffDetailsLoops;
	}

	public void setStopOffDetailsLoops(Collection<StopOffDetailsLoop> stopOffDetailsLoops) {
		this.stopOffDetailsLoops = stopOffDetailsLoops;
	}

	@Override
	public String toString() {
		return "Detail [stopOffDetailsLoops=" + stopOffDetailsLoops + "]";
	}
}
