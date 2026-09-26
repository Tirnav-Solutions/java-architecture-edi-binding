package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.model.x12.segment.Name;
import javax.edi.model.x12.segment.AddressInformation;
import javax.edi.model.x12.segment.GeographicLocation;
import javax.edi.model.x12.segment.ContactInformation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class StopOffNameLoop {

	@NotNull
	private Name name;

	@NotNull
	@EDICollectionType(AddressInformation.class)
	@Size(max=2)
	private Collection<AddressInformation> addressInformations;

	@NotNull
	private GeographicLocation geographicLocation;

	@EDICollectionType(ContactInformation.class)
	@Size(max=3)
	private Collection<ContactInformation> contactInformations;

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public Collection<AddressInformation> getAddressInformations() {
		return addressInformations;
	}

	public void setAddressInformations(Collection<AddressInformation> addressInformations) {
		this.addressInformations = addressInformations;
	}

	public GeographicLocation getGeographicLocation() {
		return geographicLocation;
	}

	public void setGeographicLocation(GeographicLocation geographicLocation) {
		this.geographicLocation = geographicLocation;
	}

	public Collection<ContactInformation> getContactInformations() {
		return contactInformations;
	}

	public void setContactInformations(Collection<ContactInformation> contactInformations) {
		this.contactInformations = contactInformations;
	}
}
