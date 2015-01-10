package org.fastnate.generator.test.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

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
		write(subEntity);
		final MappedSubclassTestEntity superEntity = new MappedSubclassTestEntity("Main entity", "property2");
		write(superEntity);

		final SingleTableSubclassTestEntity foundSubEntity = findSingleResult(SingleTableSubclassTestEntity.class);
		assertThat(foundSubEntity.getName()).isEqualTo(subEntity.getName());
		assertThat(foundSubEntity.getDescription()).isEqualTo(subEntity.getDescription());
		assertThat(foundSubEntity.getSuperProperty()).isEqualTo(subEntity.getSuperProperty());

		final List<MappedSubclassTestEntity> foundEntities = findResults(MappedSubclassTestEntity.class);
		assertThat(foundEntities).hasSize(2);
		MappedSubclassTestEntity foundSuperEntity;
		if (foundEntities.get(0).equals(foundSubEntity)) {
			assertThat(foundEntities.get(0)).isInstanceOf(SingleTableSubclassTestEntity.class);
			foundSuperEntity = foundEntities.get(1);
		} else {
			foundSuperEntity = foundEntities.get(0);
			assertThat(foundEntities.get(1)).isInstanceOf(SingleTableSubclassTestEntity.class);
		}
		assertThat(foundSuperEntity).isNotInstanceOf(SingleTableSubclassTestEntity.class);
		assertThat(foundSuperEntity.getName()).isEqualTo(superEntity.getName());
		assertThat(foundSuperEntity.getSuperProperty()).isEqualTo(superEntity.getSuperProperty());
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
		write(subEntity);
		final TablePerClassSuperclassTestEntity superEntity = new TablePerClassSuperclassTestEntity(1, "Super entity");
		write(superEntity);

		final TablePerClassSubclassTestEntity foundSubEntity = findSingleResult(TablePerClassSubclassTestEntity.class);
		assertThat(foundSubEntity.getId()).isEqualTo(subEntity.getId());
		assertThat(foundSubEntity.getName()).isEqualTo(subEntity.getName());
		assertThat(foundSubEntity.getDescription()).isEqualTo(subEntity.getDescription());

		final List<TablePerClassSuperclassTestEntity> foundEntities = findResults(TablePerClassSuperclassTestEntity.class);
		assertThat(foundEntities).hasSize(2);
		TablePerClassSuperclassTestEntity foundSuperEntity;
		if (foundEntities.get(0).equals(foundSubEntity)) {
			assertThat(foundEntities.get(0)).isInstanceOf(TablePerClassSubclassTestEntity.class);
			foundSuperEntity = foundEntities.get(1);
		} else {
			foundSuperEntity = foundEntities.get(0);
			assertThat(foundEntities.get(1)).isInstanceOf(TablePerClassSubclassTestEntity.class);
		}
		assertThat(foundSuperEntity).isNotInstanceOf(TablePerClassSubclassTestEntity.class);
		assertThat(foundSuperEntity.getName()).isEqualTo(superEntity.getName());
		assertThat(foundSuperEntity.getId()).isEqualTo(superEntity.getId());
	}

}
