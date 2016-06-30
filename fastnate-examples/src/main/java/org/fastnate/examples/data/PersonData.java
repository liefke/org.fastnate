package org.fastnate.examples.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.examples.model.Person;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Generates some example data for a {@link Person}.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class PersonData extends AbstractDataProvider {

	private final OrganisationData organisations;

	@Getter
	private final List<Person> entities = new ArrayList<>();

	/**
	 * Creates the example data.
	 */
	@Override
	public void buildEntities() throws IOException {
		final Person nate = new Person("Nate", "Smith");
		nate.setOrganisation(this.organisations.getByName().get("Fastnate"));
		this.entities.add(nate);

		final Person john = new Person("John", "Doe");
		john.setSupervisor(nate);
	}
}
