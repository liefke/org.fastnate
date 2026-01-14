package org.fastnate.generator.test.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.jupiter.api.Test;

/**
 * Test that references between entities are written correctly.
 *
 * @author Tobias Liefke
 */
public class ReferenceEntityTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Test that the references between parent and child are written in the correct order.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testRequiredParentChildReferences() throws IOException {
		final ParentTestEntity parent = new ParentTestEntity(1L, new RequiredChildTestEntity(2L, null));
		parent.getRequiredChild().setParent(parent);
		write(parent);

		final ParentTestEntity resultParent = findSingleResult(ParentTestEntity.class);
		assertThat(resultParent.getRequiredChild()).isNotNull();
		assertThat(resultParent.getRequiredChild().getParent()).isEqualTo(resultParent);
	}

}
