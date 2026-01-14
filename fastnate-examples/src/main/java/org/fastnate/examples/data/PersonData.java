package org.fastnate.examples.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.EntityRegistration;
import org.fastnate.examples.model.Organisation;
import org.fastnate.examples.model.Person;

import lombok.Getter;

/**
 * Generates some example data for a {@link Person}.
 *
 * @author Tobias Liefke
 */
public class PersonData extends AbstractDataProvider {

	/** Contains the organisations imported from organisations.csv. */
	@Resource
	private OrganisationData organisationData;

	/** Contains any entities imported by the generic importers. */
	@Resource
	private EntityRegistration entityRegistration;

	@Getter
	private final List<Person> entities = new ArrayList<>();

	/**
	 * Creates the example data.
	 */
	@Override
	public void buildEntities() throws IOException {
		final Person nate = new Person("Nate", "Smith");
		nate.setActive(true);
		nate.setOrganisation(this.organisationData.getOrganisations().get("Fastnate"));
		this.entities.add(nate);

		final Person john = new Person("John", "Doe");
		john.setSupervisor(nate);

		// Add organisation "Microsoft" to john, as soon as it was imported from the XML file
		this.entityRegistration.invokeOnEntity(Organisation.class, "Microsoft Corporation",
				john.getPreviousOrganisations()::add);
	}
}
