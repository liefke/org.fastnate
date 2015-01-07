package org.fastnate.generator.test.ids;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.fastnate.generator.AbstractEntitySqlGeneratorTest;
import org.fastnate.generator.EmbeddedTest;
import org.junit.Test;

/**
 * Tests that ids of entities are written correctly.
 *
 * The test for writing an embbeded id is located in {@link EmbeddedTest}.
 *
 * @author Tobias Liefke
 */
public class IdsTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write an entity with an identity column.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testIdentity() throws IOException {
		// Write three entities
		final IdentityTestEntity entity1 = new IdentityTestEntity("entity1");
		write(entity1);

		write(new IdentityTestEntity("entity2"));

		// And let the second one reference the first
		final IdentityTestEntity entity3 = new IdentityTestEntity("entity3");
		entity3.setOther(entity1);
		write(entity3);

		// Read the last entity
		final IdentityTestEntity foundEntity = findSingleResult(
				"SELECT e FROM IdentityTestEntity e WHERE e.name = 'entity3'", IdentityTestEntity.class);
		assertThat(foundEntity.getOther()).isNotNull();
		assertThat(foundEntity.getOther().getName()).isEqualTo("entity1");

		// And ensure that another entity may be written afterwards
		final IdentityTestEntity entity4 = new IdentityTestEntity("entity4");
		entity4.setOther(foundEntity);
		getEm().persist(entity4);
	}

	/**
	 * Tests to write an entity with a sequence generator.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testSequence() throws IOException {
		// Write three entities
		final SequenceTestEntity entity1 = new SequenceTestEntity("entity1");
		write(entity1);

		write(new SequenceTestEntity("entity2"));

		// And let the second one reference the first
		final SequenceTestEntity entity3 = new SequenceTestEntity("entity3");
		entity3.setOther(entity1);
		write(entity3);

		// Read the last entity
		final SequenceTestEntity foundEntity = findSingleResult(
				"SELECT e FROM SequenceTestEntity e WHERE e.name = 'entity3'", SequenceTestEntity.class);
		assertThat(foundEntity.getOther()).isNotNull();
		assertThat(foundEntity.getOther().getName()).isEqualTo("entity1");

		// And ensure that another entity may be written afterwards
		final SequenceTestEntity entity4 = new SequenceTestEntity("entity4");
		entity4.setOther(foundEntity);
		getEm().persist(entity4);
	}

}
