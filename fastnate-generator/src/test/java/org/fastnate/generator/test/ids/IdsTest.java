package org.fastnate.generator.test.ids;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Constructor;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.fastnate.generator.test.embedded.EmbeddedTest;
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
	 * Tests to write an entity with just the generated ID.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testEmptyEntity() throws Exception {
		write(new IdentityTestEntity(null));

		final IdentityTestEntity result = findSingleResult(IdentityTestEntity.class);
		assertThat(result.getName()).isNull();
	}

	/**
	 * Tests to write entities with fixed ids.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testFixedId() throws Exception {
		final FixedIdTestEntity foundEntity = testIds(FixedIdTestEntity.class);
		assertThat(foundEntity.getId()).isEqualTo(foundEntity.getName());
	}

	/**
	 * Tests to write an entity with an identity column.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testIdentityGenerator() throws Exception {
		testIds(IdentityTestEntity.class);
	}

	private <E extends IdTestEntity<E>> E testIds(final Class<E> entityClass) throws IOException,
			ReflectiveOperationException {
		final Constructor<E> entityConstructor = entityClass.getConstructor(String.class);
		// Write three entities
		final E entity1 = entityConstructor.newInstance("entity1");
		write(entity1);

		write(entityConstructor.newInstance("entity2"));

		// And let the third one reference the first one
		final E entity3 = entityConstructor.newInstance("entity3");
		entity3.setOther(entity1);
		write(entity3);

		// Read and check the last entity
		final E foundEntity = findSingleResult("SELECT e FROM " + entityClass.getSimpleName()
				+ " e WHERE e.name = 'entity3'", entityClass);
		assertThat(foundEntity.getOther()).isNotNull();
		assertThat(foundEntity.getOther().getName()).isEqualTo("entity1");

		// And ensure that another entity may be written afterwards
		final E entity4 = entityConstructor.newInstance("entity4");
		entity4.setOther(foundEntity);
		getEm().persist(entity4);

		return foundEntity;
	}

	/**
	 * Tests to write an entity with a sequence generator.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testSequenceGenerator() throws Exception {
		testIds(SequenceTestEntity.class);
	}

}
