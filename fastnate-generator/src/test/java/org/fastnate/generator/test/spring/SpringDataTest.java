package org.fastnate.generator.test.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;
import org.springframework.data.jpa.domain.AbstractPersistable;

/**
 * Contains tests for compatibility with Spring Data JPA.
 *
 * @author Tobias Liefke
 */
public class SpringDataTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Check that we can write a simple entity that extends {@link AbstractPersistable}.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testSimpleEntity() throws IOException {
		final SpringDataTestEntity entity = new SpringDataTestEntity("Example");
		write(entity);

		final SpringDataTestEntity result = findSingleResult(SpringDataTestEntity.class);
		assertThat(result.getContent()).isEqualTo(entity.getContent());
	}
}
