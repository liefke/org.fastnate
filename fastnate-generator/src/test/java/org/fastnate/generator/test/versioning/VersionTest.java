package org.fastnate.generator.test.versioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.persistence.Version;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that a property marked with {@link Version} is written correctly.
 *
 * @author Tobias Liefke
 */

public class VersionTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write an entity with {@code @Version}.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testVersion() throws IOException {
		// Check that version is iniatialized
		final VersionTestEntity entity = new VersionTestEntity();
		entity.setContent("before");
		write(entity);

		getGenerator().flush();
		getEm().getTransaction().begin();
		final VersionTestEntity result = findSingleResult(VersionTestEntity.class);
		assertThat(result.getVer()).isEqualTo(0);

		// And check that the JPA framework can still write our entity
		result.setContent("after");
		getEm().merge(result);
		getEm().getTransaction().commit();

		final VersionTestEntity result2 = findSingleResult(VersionTestEntity.class);
		assertThat(result2.getVer()).isEqualTo(1);
	}

}
