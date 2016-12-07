package org.fastnate.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.fastnate.data.test.TestData;
import org.fastnate.data.test.TestEntity;
import org.fastnate.generator.context.GeneratorContext;
import org.hibernate.internal.SessionImpl;
import org.junit.Test;

/**
 * Tests the {@link EntityImporter}.
 *
 * @author Tobias Liefke
 */
public class EntityImporterTest {

	private static Properties createDefaultSettings() {
		final Properties settings = new Properties();
		settings.setProperty(EntityImporter.PACKAGES_KEY, TestData.class.getPackage().getName());
		settings.setProperty(EntityImporter.DATA_FOLDER_KEY, "src/test/data");
		settings.setProperty(GeneratorContext.RELATIVE_IDS_KEY, "true");
		return settings;
	}

	/**
	 * Tests the SQL generation using the Entity Importer.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 * @throws SQLException
	 *             if the connection throws one
	 */
	@Test
	public void testConnection() throws IOException, SQLException {
		final Properties settings = createDefaultSettings();
		final EntityImporter entityImporter = new EntityImporter(settings);

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory("test", settings);
		try {
			final EntityManager em = emf.createEntityManager();
			try {
				try (Connection connection = em.unwrap(SessionImpl.class).getJdbcConnectionAccess()
						.obtainConnection()) {
					entityImporter.importData(connection);
					connection.commit();
				}

				// Check that all entities found their way to the database
				final List<TestEntity> entities = em.createQuery("SELECT e FROM TestEntity e", TestEntity.class)
						.getResultList();

				// TestData + SuccessorData + CsvTestData
				final int expectedEntities = 3 + 1 + 2;
				assertThat(entities).hasSize(expectedEntities);
			} finally {
				em.close();
			}
		} finally {
			emf.close();
		}
	}

	/**
	 * Tests the SQL generation to a file.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testFile() throws IOException {
		final Properties settings = createDefaultSettings();

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
		assertThat(sql).contains("INSERT INTO TestEntity (bool, name, integ, parent_id) "
				+ "VALUES (0, 'CSV Child;Example', 0, (SELECT max(id) FROM TestEntity))");
	}

}
