package org.fastnate.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.fastnate.generator.testmodel.PrimitiveTestEntity;
import org.fastnate.generator.testmodel.TestEmbeddedId;
import org.fastnate.generator.testmodel.TestEmbeddedProperties;
import org.fastnate.generator.testmodel.TestEmbeddingEntity;
import org.fastnate.generator.testmodel.TestRequiredEmbeddedProperties;
import org.junit.Test;

/**
 * Contains tests for embedded entities.
 *
 * @author Tobias Liefke
 */
public class EmbeddedTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write embedded properties.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testEmbedded() throws IOException {
		final TestEmbeddingEntity testEntity = new TestEmbeddingEntity();
		testEntity.setId(new TestEmbeddedId(2, "Test embedding"));
		final TestEmbeddedProperties testProperties = new TestEmbeddedProperties("Example", new PrimitiveTestEntity(
				"embedded child"));
		testEntity.setProperties(testProperties);
		testEntity.setRequired(new TestRequiredEmbeddedProperties("req", "opt"));
		write(testEntity);

		final TestEmbeddingEntity result = findSingleResult("SELECT e FROM EmbedEnty e WHERE e.id.id = 2",
				TestEmbeddingEntity.class);
		assertThat(result.getId()).isEqualTo(testEntity.getId());
		final TestEmbeddedProperties resultProperties = result.getProperties();
		assertThat(resultProperties.getDescription()).isEqualTo(testProperties.getDescription());
		assertThat(resultProperties.getOtherNode().getName()).isEqualTo(testProperties.getOtherNode().getName());

		assertThat(result.getRequired().getRequired()).isEqualTo(testEntity.getRequired().getRequired());
	}

}
