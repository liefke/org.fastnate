package org.fastnate.generator.test.ids;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.fastnate.generator.context.GeneratorContext;

/**
 * Tests that relative ids of entities are written correctly.
 *
 * @author Tobias Liefke
 */
public class RelativeIdsTest extends IdsTest {

	/**
	 * Configure our generator to use relative IDs.
	 */
	@Override
	protected Properties getGeneratorProperties() {
		final Properties properties = super.getGeneratorProperties();
		properties.setProperty(GeneratorContext.RELATIVE_IDS_KEY, "true");
		return properties;
	}

	@Override
	protected <E extends IdTestEntity<E>> E testIds(final Class<E> entityClass, final String prefix)
			throws IOException, ReflectiveOperationException, SQLException {
		// First test with an empty database
		E result = super.testIds(entityClass, "empty" + prefix);

		if (result.getId() instanceof Number) {
			// Now test with an existing database
			tearDown();
			setup("");
			result = super.testIds(entityClass, "nonEmpty" + prefix);
		}

		return result;
	}

}
