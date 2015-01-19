package org.fastnate.generator.test.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that access types are read correctly.
 *
 * @author Tobias Liefke
 */
public class AccessTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests that an explict access type is taken into account.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testExplicitAccess() throws IOException {
		final ExplicitAccessTestEntity entity = new ExplicitAccessTestEntity(0, "Explicit");

		write(entity);

		final ExplicitAccessTestEntity result = findSingleResult(ExplicitAccessTestEntity.class);
		assertThat(result.getId()).isEqualTo(entity.getId());
		assertThat(result.getName()).isEqualTo(entity.getName());
	}

	/**
	 * Tests that an implict access type is taken into account.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testImplicitAccess() throws IOException {
		final ImplicitAccessTestEntity entity = new ImplicitAccessTestEntity("Explicit");

		write(entity);

		final ImplicitAccessTestEntity result = findSingleResult(ImplicitAccessTestEntity.class);
		assertThat(result.getName()).isEqualTo(entity.getName());
	}

}
