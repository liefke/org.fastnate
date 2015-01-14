package org.fastnate.generator.test.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import javax.persistence.InheritanceType;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that inheritance is correctly taken into account.
 *
 * @author Tobias Liefke
 */
public class InheritanceTest extends AbstractEntitySqlGeneratorTest {

	private void testInheritance(final SubclassEntity subEntity, final SuperclassEntity superEntity) throws IOException {
		write(subEntity);
		write(superEntity);

		final Class<? extends SubclassEntity> subclass = subEntity.getClass();
		final Class<? extends SuperclassEntity> superclass = superEntity.getClass();
		final SubclassEntity foundSubEntity = findSingleResult(subclass);
		assertThat(foundSubEntity.getName()).isEqualTo(subEntity.getName());
		assertThat(foundSubEntity.getDescription()).isEqualTo(subEntity.getDescription());
		assertThat(foundSubEntity.getSuperProperty()).isEqualTo(subEntity.getSuperProperty());

		final List<? extends SuperclassEntity> foundEntities = findResults(superclass);
		assertThat(foundEntities).hasSize(2);
		SuperclassEntity foundSuperEntity;
		if (foundEntities.get(0).equals(foundSubEntity)) {
			assertThat(foundEntities.get(0)).isInstanceOf(subclass);
			foundSuperEntity = foundEntities.get(1);
		} else {
			foundSuperEntity = foundEntities.get(0);
			assertThat(foundEntities.get(1)).isInstanceOf(subclass);
		}
		assertThat(foundSuperEntity).isNotInstanceOf(subclass);
		assertThat(foundSuperEntity.getName()).isEqualTo(superEntity.getName());
		assertThat(foundSuperEntity.getSuperProperty()).isEqualTo(superEntity.getSuperProperty());
	}

	/**
	 * Tests to write an entity hierarchy with {@link InheritanceType#JOINED}.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testJoinedInheritance() throws Exception {
		final JoinedSubclassTestEntity subEntity = new JoinedSubclassTestEntity("Sub entity", "Saved to sub table",
				"Saved to master table for sub entity");
		final JoinedSuperclassTestEntity superEntity = new JoinedSuperclassTestEntity("Super entity",
				"Saved to master table for super entity");
		testInheritance(subEntity, superEntity);
	}

	/**
	 * Tests to write sub classes of mapped superclasses.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testMappedSuperclasses() throws Exception {
		final MappedSubclassTestEntity testEntity = new MappedSubclassTestEntity("entity1", "mapped");
		write(testEntity);

		final MappedSubclassTestEntity foundEntity = findSingleResult(MappedSubclassTestEntity.class);

		assertThat(foundEntity.getName()).isEqualTo(testEntity.getName());
		assertThat(foundEntity.getSuperProperty()).isEqualTo(testEntity.getSuperProperty());
	}

	/**
	 * Tests to write an entity hierarchy with {@link InheritanceType#SINGLE_TABLE}.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testSingleTableInheritance() throws Exception {
		final SingleTableSubclassTestEntity subEntity = new SingleTableSubclassTestEntity("Sub entity",
				"The inherited entity", "property1");
		final MappedSubclassTestEntity superEntity = new MappedSubclassTestEntity("Main entity", "property2");

		testInheritance(subEntity, superEntity);
	}

	/**
	 * Tests to write an entity hierarchy with {@link InheritanceType#TABLE_PER_CLASS}.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testTablePerClassInheritance() throws Exception {
		final TablePerClassSubclassTestEntity subEntity = new TablePerClassSubclassTestEntity(0, "Sub entity",
				"The inherited entity");
		final TablePerClassSuperclassTestEntity superEntity = new TablePerClassSuperclassTestEntity(1, "Super entity");
		testInheritance(subEntity, superEntity);
	}

}
