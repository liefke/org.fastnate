package org.fastnate.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.fastnate.generator.testmodel.TestRecursiveEntity;
import org.junit.Test;

/**
 * Test that recursions are written correctly.
 *
 * @author Tobias Liefke
 */
public class RecursiveEntityTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write recursion.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testRecursion() throws IOException {
		// Check that both nodes are written, no matter which order we use
		final TestRecursiveEntity root = new TestRecursiveEntity(null, "Test Recursion Root");
		final TestRecursiveEntity child = new TestRecursiveEntity(root, "Test Recursion Child");
		write(root);

		final String query = "SELECT e FROM TestRecursiveEntity e WHERE e.name = :name";
		final TestRecursiveEntity writtenChild = getEm().createQuery(query, TestRecursiveEntity.class)
				.setParameter("name", child.getName()).getSingleResult();
		assertThat(writtenChild.getChildren()).isEmpty();
		final TestRecursiveEntity writtenRoot = writtenChild.getParent();
		assertThat(writtenRoot.getName()).isEqualTo(root.getName());
		assertThat(writtenRoot.getParent()).isNull();
		assertThat(writtenRoot.getChildren()).contains(writtenChild);

		final TestRecursiveEntity root2 = new TestRecursiveEntity(null, "Test Recursion Root2");
		final TestRecursiveEntity child2 = new TestRecursiveEntity(root2, "Test Recursion Child2");
		write(child2);

		final TestRecursiveEntity writtenRoot2 = getEm().createQuery(query, TestRecursiveEntity.class)
				.setParameter("name", root2.getName()).getSingleResult();
		assertThat(writtenRoot2.getParent()).isNull();
		assertThat(writtenRoot2.getChildren()).hasSize(1);
		final TestRecursiveEntity writtenChild2 = writtenRoot2.getChildren().iterator().next();
		assertThat(writtenChild2.getName()).isEqualTo(child2.getName());
	}

}
