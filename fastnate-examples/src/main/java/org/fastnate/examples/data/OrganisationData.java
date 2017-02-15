package org.fastnate.examples.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.fastnate.data.csv.AbstractCsvDataProvider;
import org.fastnate.examples.model.Organisation;

import lombok.Getter;

/**
 * Example DataProvider for importing a CSV file.
 *
 * @author Tobias Liefke
 */
@Getter
public class OrganisationData extends AbstractCsvDataProvider<Organisation> {

	private final Map<String, Organisation> byName = new HashMap<>();

	/**
	 * Creates a new instance of {@link OrganisationData}.
	 *
	 * @param importPath
	 *            the path to the directory of organisation.csv
	 */
	public OrganisationData(final File importPath) {
		super(new File(importPath, "organisations.csv"));

		// Map the "web" column to the "url" property
		addColumnMapping("web", "url");

		// Ignore the comment column
		addIgnoredColumn("comment");
	}

	@Override
	protected Organisation createEntity(final Map<String, String> row) {
		final Organisation organisation = super.createEntity(row);

		// Remember every organisation by its name
		this.byName.put(organisation.getName(), organisation);

		return organisation;
	}

}
