package org.fastnate.generator.test.schema;

import java.io.IOException;

import jakarta.persistence.Table;

import org.assertj.core.api.Assertions;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that {@link Table#schema() schema} and {@link Table#catalog() catalog} annotations are respected correctly.
 *
 * @author Tobias Liefke
 */
public class SchemaTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Ensures that {@link Table#schema() schema} and {@link Table#catalog() catalog} annotations are respected
	 * correctly.
	 *
	 * @throws IOException
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testSchema() throws IOException {
		final SchemaTestEntity entity = new SchemaTestEntity("Parent");
		entity.getEntities().add(new SchemaTestEntity("Child"));
		entity.getStrings().add("Singlestring");
		write(entity);

		final SchemaTestEntity result = findSingleResult("SELECT e FROM SchemaTestEntity e WHERE e.name = :name",
				SchemaTestEntity.class, "name", entity.getName());
		Assertions.assertThat(result.getName()).isEqualTo(entity.getName());
		Assertions.assertThat(result.getEntities()).hasSameSizeAs(entity.getEntities());
		Assertions.assertThat(result.getEntities().iterator().next().getName())
				.isEqualTo(entity.getEntities().iterator().next().getName());
		Assertions.assertThat(result.getStrings()).containsExactlyElementsOf(entity.getStrings());
	}

}
