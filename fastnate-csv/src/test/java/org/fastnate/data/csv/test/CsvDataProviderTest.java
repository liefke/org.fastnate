package org.fastnate.data.csv.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.fastnate.data.EntityImporter;
import org.fastnate.data.csv.CsvDataImporter;
import org.fastnate.data.csv.GenericCsvDataProvider;
import org.fastnate.generator.context.GeneratorContext;
import org.junit.Test;

/**
 * Tests the {@link GenericCsvDataProvider}.
 *
 * @author Tobias Liefke
 */
public class CsvDataProviderTest {

	private static Properties createDefaultSettings() {
		final Properties settings = new Properties();
		settings.setProperty(EntityImporter.DATA_FOLDER_KEY, "data");
		settings.setProperty(GeneratorContext.RELATIVE_IDS_KEY, "true");
		settings.setProperty(CsvDataImporter.COLUMN_DELIMITER, ";");
		return settings;
	}

	/**
	 * Tests the SQL generation from a CSV file.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testImport() throws IOException {
		final Properties settings = createDefaultSettings();

		final EntityImporter entityImporter = new EntityImporter(settings);
		final StringWriter sqlWriter = new StringWriter();
		entityImporter.importData(sqlWriter);
		final String sql = StringUtils.normalizeSpace(sqlWriter.toString().replaceAll("(?m)^/\\*.*", "").trim());

		// Check CSVData
		assertThat(sql).isEqualTo("INSERT INTO TestEntity (name, integ) VALUES ('CSV Root 2', 1);"
				+ " INSERT INTO TestEntity (name, integ, parent_id)"
				+ " VALUES ('Second child', 12, (SELECT max(id) FROM TestEntity));"
				+ " INSERT INTO TestEntity (name, integ, parent_id)"
				+ " VALUES ('First child', 11, (SELECT max(id) - 1 FROM TestEntity));"
				+ " INSERT INTO TestEntity (name, integ, parent_id)"
				+ " VALUES ('Second sub child', 112, (SELECT max(id) FROM TestEntity));"
				+ " INSERT INTO TestEntity (name, integ, parent_id)"
				+ " VALUES ('First sub child', 111, (SELECT max(id) - 1 FROM TestEntity));"
				+ " INSERT INTO TestEntity (bool, date, name, integ)"
				+ " VALUES (1, '2000-01-01 00:00:00.0', 'CSV Root', 0);"
				+ " INSERT INTO TestEntity (bool, date, name, integ, parent_id)"
				+ " VALUES (0, '2010-12-31 23:23:59.0', 'CSV Child;Example', 1, (SELECT max(id) FROM TestEntity));");
	}

}
