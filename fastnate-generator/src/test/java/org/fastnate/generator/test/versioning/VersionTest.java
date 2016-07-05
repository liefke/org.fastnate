package org.fastnate.generator.test.versioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.persistence.Version;

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
		// Check that both version is iniatialized
		final VersionTestEntity entity = new VersionTestEntity();
		entity.setContent("before");
		write(entity);

		getEm().getTransaction().begin();
		final VersionTestEntity result = findSingleResult(VersionTestEntity.class);
		assertThat(result.getVer()).isEqualTo(0);

		// And check that hibernate can still write our entity
		result.setContent("after");
		getEm().merge(result);
		getEm().getTransaction().commit();

		final VersionTestEntity result2 = findSingleResult(VersionTestEntity.class);
		assertThat(result2.getVer()).isEqualTo(1);
	}

}
