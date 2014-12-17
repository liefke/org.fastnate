package org.fastnate.examples.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.fastnate.data.DataProvider;
import org.fastnate.examples.model.Person;

/**
 * Generates some example data for a {@link Person}.
 *
 * @author Tobias Liefke
 */
public class PersonData implements DataProvider {

	@Getter
	private final List<Person> entities = new ArrayList<>();

	/**
	 * Creates the example data.
	 */
	@Override
	public void buildEntities() throws IOException {
		this.entities.add(new Person("Nate", "Smith"));
	}
}
