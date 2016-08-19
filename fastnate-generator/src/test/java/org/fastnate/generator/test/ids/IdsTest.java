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
		if (getGenerator().getContext().getDialect().isIdentitySupported()) {
			write(new IdentityTestEntity(null));

			final IdentityTestEntity result = findSingleResult(IdentityTestEntity.class);
			assertThat(result.getName()).isNull();
		}
	}

	/**
	 * Tests to write entities with fixed ids.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testFixedId() throws Exception {
		final FixedIdTestEntity foundEntity = testIds(FixedIdTestEntity.class, "fixedIdEntity");
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
		if (getGenerator().getContext().getDialect().isIdentitySupported()) {
			testIds(IdentityTestEntity.class, "entity");
		}
	}

	/**
	 * Tests to write entities of the given class.
	 *
	 * @param entityClass
	 *            the type of the entity
	 * @param entityPrefix
	 *            an option Prefix for the entity names to allow more than one call of this method
	 *
	 * @return the first written entity as read by Hibernate
	 * @throws IOException
	 *             if the generator throws one
	 * @throws ReflectiveOperationException
	 *             if we can't create one of the entities
	 */
	protected <E extends IdTestEntity<E>> E testIds(final Class<E> entityClass, final String entityPrefix)
			throws IOException, ReflectiveOperationException {
		final Constructor<E> entityConstructor = entityClass.getConstructor(String.class);
		// Write three entities
		final E entity1 = entityConstructor.newInstance(entityPrefix + "1");
		write(entity1);

		write(entityConstructor.newInstance(entityPrefix + "2"));

		// And let the third one reference the first one
		final E entity3 = entityConstructor.newInstance(entityPrefix + "3");
		entity3.setOther(entity1);
		write(entity3);

		getGenerator().writeAlignmentStatements();

		// Read and check the last entity
		final E foundEntity = findSingleResult(
				"SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.name = '" + entityPrefix + "3'",
				entityClass);
		assertThat(foundEntity.getOther()).isNotNull();
		assertThat(foundEntity.getOther().getName()).isEqualTo(entityPrefix + "1");

		// Check that we create the IDs correctly
		final boolean checkIncreasingIds = foundEntity.getId() instanceof Number;
		if (checkIncreasingIds) {
			assertThat(((Number) foundEntity.getId()).longValue())
					.isEqualTo(((Number) foundEntity.getOther().getId()).longValue() + 2);
		}

		getEm().getTransaction().begin();

		// And ensure that another entity may be written afterwards
		final E entity4 = entityConstructor.newInstance(entityPrefix + "4");
		entity4.setOther(foundEntity);
		getEm().persist(entity4);

		getEm().getTransaction().commit();

		if (checkIncreasingIds && entityClass != SequenceTestEntity.class) {
			assertThat(((Number) entity4.getId()).longValue())
					.isEqualTo(((Number) foundEntity.getId()).longValue() + 1);
		}

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
		if (getGenerator().getContext().getDialect().isSequenceSupported()) {
			testIds(SequenceTestEntity.class, "sequenceEntity");
		}
	}

	/**
	 * Tests to write an entity with a table generator.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testTableGenerator() throws Exception {
		testIds(TableTestEntity.class, "tableEntity");
	}

}
