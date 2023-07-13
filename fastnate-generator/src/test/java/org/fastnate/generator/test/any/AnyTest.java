package org.fastnate.generator.test.any;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.fastnate.generator.test.SimpleTestEntity;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.junit.Test;

/**
 *
 * @author Tobias Liefke
 */
public class AnyTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests that all "Any" annotations are interpreted correctly.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testAnyFields() throws IOException {
		final AnyContainer entity = new AnyContainer();
		final SimpleTestEntity singleAny = new SimpleTestEntity("AnyTest");
		entity.setSingleAny(singleAny);
		final SimpleTestEntity firstManyAny = new SimpleTestEntity("ManyAny");
		entity.setManyAny(Arrays.asList(firstManyAny, entity));
		entity.setAnyMap(Collections.singletonMap("AnyMapEntityKey", new SimpleTestEntity("AnyMapEntity")));

		write(entity);

		final AnyContainer result = findSingleResult(AnyContainer.class);
		assertThat(result.getSingleAny()).isInstanceOf(SimpleTestEntity.class);
		assertThat(result.getSingleAny()).isEqualTo(singleAny);
		assertThat(((SimpleTestEntity) result.getSingleAny()).getName()).isEqualTo(singleAny.getName());
		assertThat(result.getManyAny()).hasSize(2);
		assertThat(((SimpleTestEntity) result.getManyAny().get(0)).getName()).isEqualTo(firstManyAny.getName());
		assertThat(result.getManyAny().get(1)).isEqualTo(result);
		assertThat(result.getAnyMap().entrySet()).containsOnlyElementsOf(entity.getAnyMap().entrySet());
	}

}
