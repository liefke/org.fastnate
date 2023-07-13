package org.fastnate.generator.test.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import jakarta.persistence.InheritanceType;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that inheritance is correctly taken into account.
 *
 * @author Tobias Liefke
 */
public class InheritanceTest extends AbstractEntitySqlGeneratorTest {

	private <T extends SuperclassEntity> T testInheritance(final T superEntity, final SubclassEntity... subEntities)
			throws IOException {
		write(superEntity);
		for (final SubclassEntity subEntity : subEntities) {
			write(subEntity);
		}

		final Class<? extends SuperclassEntity> superclass = superEntity.getClass();
		for (final SubclassEntity subEntity : subEntities) {
			final Class<? extends SubclassEntity> subclass = subEntity.getClass();
			final SubclassEntity foundSubEntity = findSingleResult(subclass);
			assertThat(foundSubEntity.getName()).isEqualTo(subEntity.getName());
			assertThat(foundSubEntity.getDescription()).isEqualTo(subEntity.getDescription());
			assertThat(foundSubEntity.getSuperProperty()).isEqualTo(subEntity.getSuperProperty());
			assertThat(superclass).isAssignableFrom(subclass);
		}

		final List<? extends SuperclassEntity> foundEntities = findResults(superclass);
		assertThat(foundEntities).hasSize(1 + subEntities.length);
		final T foundSuperEntity = (T) foundEntities.stream().filter(superEntity::equals).findFirst()
				.orElseThrow(AssertionError::new);
		assertThat(foundSuperEntity).hasSameClassAs(superEntity);
		assertThat(foundSuperEntity.getName()).isEqualTo(superEntity.getName());
		assertThat(foundSuperEntity.getSuperProperty()).isEqualTo(superEntity.getSuperProperty());
		return foundSuperEntity;
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
		superEntity.setSuperReference(subEntity);
		superEntity.setSubReference(subEntity);
		final JoinedSuperclassTestEntity foundSuperEntity = testInheritance(superEntity, subEntity);
		assertThat(foundSuperEntity.getSuperReference()).isInstanceOf(JoinedSubclassTestEntity.class);
		assertThat(foundSuperEntity.getSuperReference().getName()).isEqualTo(subEntity.getName());
		assertThat(foundSuperEntity.getSubReference().getName()).isEqualTo(subEntity.getName());
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
		testEntity.setGenericProperty(1);
		write(testEntity);

		final MappedSubclassTestEntity foundEntity = findSingleResult(MappedSubclassTestEntity.class);

		assertThat(foundEntity.getName()).isEqualTo(testEntity.getName());
		assertThat(foundEntity.getSuperProperty()).isEqualTo(testEntity.getSuperProperty());
		assertThat(foundEntity.getGenericProperty()).isEqualByComparingTo(testEntity.getGenericProperty());
	}

	/**
	 * Tests to write an entity hierarchy with {@link InheritanceType#SINGLE_TABLE}.
	 *
	 * @throws Exception
	 *             if Hibernate or the generator throws one
	 */
	@Test
	public void testSingleTableInheritance() throws Exception {
		final MappedSubclassTestEntity superEntity = new MappedSubclassTestEntity("Main entity", "property.main");
		final SingleTableSubclassTestEntity subEntity = new SingleTableSubclassTestEntity("Sub entity",
				"The inherited entity", "property.sub");
		final SecondSingleTableSubclassTestEntity secondEntity = new SecondSingleTableSubclassTestEntity(
				"Second sub entity", 2, "property.second");
		testInheritance(superEntity, subEntity, secondEntity);

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
		testInheritance(superEntity, subEntity);
	}

}
