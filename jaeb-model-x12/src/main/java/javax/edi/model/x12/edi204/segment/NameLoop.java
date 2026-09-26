package javax.edi.model.x12.edi204.segment;

import java.util.Collection;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.model.x12.segment.AddressInformation;
import javax.edi.model.x12.segment.ContactInformation;
import javax.edi.model.x12.segment.GeographicLocation;
import javax.edi.model.x12.segment.Name;
import javax.edi.model.x12.segment.PersonContact;
import javax.validation.constraints.Size;

@EDISegmentGroup
public class NameLoop {

	private Name name;

	@EDICollectionType(AddressInformation.class)
	@Size(max=2)
	private Collection<AddressInformation> addressInformations;

	private GeographicLocation geographicLocation;

	@EDICollectionType(ContactInformation.class)
	@Size(max=3)
	private Collection<ContactInformation> personContacts;

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

	public Collection<ContactInformation> getPersonContacts() {
		return personContacts;
	}

	public void setPersonContacts(Collection<ContactInformation> personContacts) {
		this.personContacts = personContacts;
	}
}
