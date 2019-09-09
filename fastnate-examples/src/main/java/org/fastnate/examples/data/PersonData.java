package org.fastnate.examples.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.EntityRegistration;
import org.fastnate.examples.model.Person;

import lombok.Getter;

/**
 * Generates some example data for a {@link Person}.
 *
 * @author Tobias Liefke
 */
public class PersonData extends AbstractDataProvider {

	/** The entities that were found by the generic importers. */
	@Resource
	private EntityRegistration entityRegistration;

	/** Contains the organisations imported from organisations.csv. */
	@Resource
	private OrganisationData organisationData;

	@Getter
	private final List<Person> entities = new ArrayList<>();

	/**
	 * Creates the example data.
	 */
	@Override
	public void buildEntities() throws IOException {
		final Person nate = new Person("Nate", "Smith");
		nate.setOrganisation(this.organisationData.getOrganisations().get("Fastnate"));
		this.entities.add(nate);

		final Person john = new Person("John", "Doe");
		john.setSupervisor(nate);
	}
}
