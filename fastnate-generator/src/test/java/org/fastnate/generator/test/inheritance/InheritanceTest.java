package org.fastnate.generator.test.inheritance;

import org.assertj.core.api.Assertions;
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
	public void testIdentityGenerator() throws Exception {
		final MappedSubclassTestEntity testEntity = new MappedSubclassTestEntity("entity1", "mapped");
		write(testEntity);

		final MappedSubclassTestEntity foundEntity = findSingleResult(MappedSubclassTestEntity.class);

		Assertions.assertThat(foundEntity.getName()).isEqualTo(testEntity.getName());
		Assertions.assertThat(foundEntity.getSuperProperty()).isEqualTo(testEntity.getSuperProperty());
	}

}
