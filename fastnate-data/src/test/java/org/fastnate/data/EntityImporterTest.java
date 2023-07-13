package org.fastnate.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.fastnate.data.test.InjectTestData;
import org.fastnate.data.test.TestData;
import org.fastnate.data.test.TestEntity;
import org.fastnate.generator.context.GeneratorContext;
import org.hibernate.internal.SessionImpl;
import org.junit.Test;
import org.reflections.Reflections;

/**
 * Tests the {@link EntityImporter}.
 *
 * @author Tobias Liefke
 */
public class EntityImporterTest {

	/** Tests the {@link DefaultDataProviderFactory}, but removes the {@link InjectTestData}. */
	public static final class TestDefaultDataProviderFactory extends DefaultDataProviderFactory {

		@Override
		protected List<Class<? extends DataProvider>> findProviderClasses(final Reflections reflections) {
			final List<Class<? extends DataProvider>> providerClasses = super.findProviderClasses(reflections);
			providerClasses.remove(InjectTestData.class);
			return providerClasses;
		}

	}

	private static Properties createDefaultSettings() {
		final Properties settings = new Properties();
		settings.setProperty(EntityImporter.PACKAGES_KEY, TestData.class.getPackage().getName());
		settings.setProperty(EntityImporter.DATA_FOLDER_KEY, "src/test/data");
		settings.setProperty(GeneratorContext.RELATIVE_IDS_KEY, "true");
		return settings;
	}

	private static void testFile(final Class<? extends DataProviderFactory> factoryClass, final String sqlSuffix)
			throws IOException {
		final Properties settings = createDefaultSettings();
		settings.setProperty(EntityImporter.FACTORY_KEY, factoryClass.getName());

		final String prefix = "// This is the prefix";
		settings.setProperty(EntityImporter.PREFIX_KEY, prefix);
		final String postfix = "// This is the postfix";
		settings.setProperty(EntityImporter.POSTFIX_KEY, postfix);

		final EntityImporter entityImporter = new EntityImporter(settings);

		final StringWriter sqlWriter = new StringWriter();
		entityImporter.importData(sqlWriter);

		// Normalize string
		final String sql = sqlWriter.toString().replaceAll("/\\*.*?\\*/", "").replaceAll("\\s+", " ").trim();

		// Check prefix and postfix
		assertThat(sql).startsWith(prefix);
		assertThat(sql).endsWith(postfix);

		final String content = sql.substring(prefix.length(), sql.length() - postfix.length()).trim();

		assertThat(content).isEqualTo(""
				// XML Generic import
				+ "INSERT INTO TestEntity (bool, name, integ) VALUES (1, 'XML Root 1', 1);"
				+ " INSERT INTO TestEntity (bool, name, integ, parent_id)"
				+ " VALUES (0, 'XML Child 2', 12, (SELECT max(id) FROM TestEntity));"
				+ " INSERT INTO TestEntity (bool, name, integ, parent_id)"
				+ " VALUES (0, 'XML Child 1', 11, (SELECT max(id) - 1 FROM TestEntity));"
				+ " INSERT INTO TestEntity (bool, name, integ, parent_id)"
				+ " VALUES (0, 'XML Sub Child 1', 111, (SELECT max(id) FROM TestEntity));"
				+ " INSERT INTO TestEntity (bool, name, integ, parent_id)"
				+ " VALUES (0, 'XML Sub Child 2', 112, (SELECT max(id) - 1 FROM TestEntity));"
				+ " INSERT INTO TestEntity (name, integ, parent_id)"
				+ " VALUES ('XML Sub Child 4', 114, (SELECT max(id) - 2 FROM TestEntity));"
				+ " INSERT INTO TestEntity (bool, name, integ, parent_id)"
				+ " VALUES (1, 'XML Sub Child 3', 113, (SELECT max(id) - 3 FROM TestEntity));"
				+ " INSERT INTO TestEntity (name, integ, parent_id)"
				+ " VALUES ('XML Sub Child 5', 115, (SELECT max(id) - 4 FROM TestEntity));"
				+ " INSERT INTO TestEntity (bool, name, integ) VALUES (1, 'XML Root 2', 2);"

				// TestData
				+ " INSERT INTO TestEntity (name) VALUES ('Root');"
				+ " INSERT INTO TestEntity (name, parent_id) VALUES ('Child1', (SELECT max(id) FROM TestEntity));"
				+ " INSERT INTO TestEntity (name, parent_id) VALUES ('Child2', (SELECT max(id) - 1 FROM TestEntity));"

				// JaxbTestEntity
				+ " INSERT INTO JaxbTestEntity (content, name) VALUES ('The example content', 'JAXB Test');"

				// DependentConstructorData
				+ " INSERT INTO TestEntity (name, parent_id)"
				+ " VALUES ('DependentConstructorChild', (SELECT max(id) - 1 FROM TestEntity));"

				// DependentResourceData
				+ " INSERT INTO TestEntity (name, parent_id)"
				+ " VALUES ('DependentResourceChild', (SELECT max(id) - 1 FROM TestEntity));"

				// The remaining SQL
				+ sqlSuffix);
	}

	/**
	 * Tests the import of entities when using a {@link EntityImporter#importData(Connection) database connection}.
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

				// TextEntity.xml + TestData + DependentConstructorData + DependentResourceData + InjectTestData
				final int expectedEntities = 9 + 3 + 1 + 1 + 2;
				assertThat(entities).hasSize(expectedEntities);
			} finally {
				em.close();
			}
		} finally {
			emf.close();
		}
	}

	/**
	 * Tests the SQL generation to a file with {@link DefaultDataProviderFactory}.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testFileDefault() throws IOException {
		testFile(TestDefaultDataProviderFactory.class, "");
	}

	/**
	 * Tests the SQL generation to a file with {@link InjectDataProviderFactory}.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testFileWithInject() throws IOException {
		testFile(InjectDataProviderFactory.class,
				// InjectTestData
				" INSERT INTO TestEntity (name, parent_id)"
						+ " VALUES ('Injected Child', (SELECT max(id) - 2 FROM TestEntity));"
						+ " INSERT INTO TestEntity (name, parent_id)"
						+ " VALUES ('Injected Child 2', (SELECT max(id) - 1 FROM TestEntity));");
	}

}
