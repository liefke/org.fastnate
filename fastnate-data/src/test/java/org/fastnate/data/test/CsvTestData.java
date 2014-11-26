package org.fastnate.data.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.fastnate.data.csv.AbstractCsvDataProvider;
import org.fastnate.data.csv.CsvMapConverter;

/**
 * Tests the import from CSV.
 *
 * @author Tobias Liefke
 */
public class CsvTestData extends AbstractCsvDataProvider<TestEntity> {

	private final Map<String, TestEntity> entities = new HashMap<>();

	/**
	 * Creates a new instance of this test class.
	 *
	 * @param dataDir
	 *            the data directory
	 */
	public CsvTestData(final File dataDir) {
		super(new File(dataDir, "csv"));

		// Map the parent column
		useTableColumns();
		addColumnMapping("parentName", "parent");
		addConverter("parentName", CsvMapConverter.create(this.entities));
	}

	@Override
	protected TestEntity createEntity(final Map<String, String> row) {
		final TestEntity entity = super.createEntity(row);

		// Remember the entity to look it up as parent
		this.entities.put(entity.getName(), entity);
		return entity;
	}

}
