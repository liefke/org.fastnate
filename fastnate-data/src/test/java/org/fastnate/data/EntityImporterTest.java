package org.fastnate.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.fastnate.data.test.TestData;
import org.junit.Test;

/**
 * Tests the entity importer.
 *
 * @author Tobias Liefke
 */
public class EntityImporterTest {

	/**
	 * Tests the SQL generation using the Entity Importer.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testEmbedded() throws IOException {
		final Properties settings = new Properties();
		settings.setProperty(EntityImporter.PACKAGES_KEY, TestData.class.getPackage().getName());
		settings.setProperty(EntityImporter.DATA_FOLDER_KEY, "src/test/data");

		final String prefix = "// This is the prefix";
		settings.setProperty(EntityImporter.PREFIX_KEY, prefix);
		final String postfix = "// This is the postfix";
		settings.setProperty(EntityImporter.POSTFIX_KEY, postfix);

		final EntityImporter entityImporter = new EntityImporter(settings);
		final StringWriter sqlWriter = new StringWriter();
		entityImporter.importData(sqlWriter);
		final String sql = sqlWriter.toString().replaceFirst("^/\\*.*", "").trim().replaceFirst("^/\\*.*", "").trim();

		// Check prefix and postfix
		assertThat(sql).startsWith(prefix);
		assertThat(sql).endsWith(postfix);

		// Check TestData and SuccessorData
		assertThat(sql).contains("INSERT INTO TestEntity (name) VALUES ('Root')");
		assertThat(sql).contains(
				"INSERT INTO TestEntity (name, parent_id) VALUES ('Successor', (SELECT max(id) - 1 FROM TestEntity))");

		// Check CSVData
		assertThat(sql).contains(
				"INSERT INTO TestEntity (bool, name, integ, parent_id) "
						+ "VALUES (0, 'CSV Child;Example', 0, (SELECT max(id) FROM TestEntity))");

	}

}
