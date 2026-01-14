package org.fastnate.generator.test.embedded;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.fastnate.generator.test.SimpleTestEntity;
import org.fastnate.generator.test.primitive.TestEnum;
import org.junit.jupiter.api.Test;

/**
 * Contains tests for embedded entities.
 *
 * @author Tobias Liefke
 */
public class EmbeddedTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write embedded properties.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testEmbedded() throws IOException {
		final TestEmbeddingEntity testEntity = new TestEmbeddingEntity();
		testEntity.setId(new TestEmbeddedId(2, "Test embedding"));
		final TestEmbeddedProperties testProperties = new TestEmbeddedProperties("Example",
				new SimpleTestEntity("Embedded child"));

		testEntity.setProperties(testProperties);

		testEntity.setRequired(new TestRequiredEmbeddedProperties("req", "opt"));

		write(testEntity);

		final TestEmbeddingEntity result = findSingleResult("SELECT e FROM EmbedEnty e WHERE e.id.id = 2",
				TestEmbeddingEntity.class);
		assertThat(result.getId()).isEqualTo(testEntity.getId());

		final TestEmbeddedProperties resultProperties = result.getProperties();
		assertThat(resultProperties.getDescription()).isEqualTo(testProperties.getDescription());
		assertThat(resultProperties.getOtherEntity().getName()).isEqualTo(testProperties.getOtherEntity().getName());

		assertThat(result.getRequired().getRequired()).isEqualTo(testEntity.getRequired().getRequired());
	}

	/**
	 * Tests to write nested embedded properties.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testNested() throws IOException {
		final TestNestedEmbeddingEntity testEntity = new TestNestedEmbeddingEntity();
		testEntity.setId(1);

		final TestNestedEmbeddedParent nested = new TestNestedEmbeddedParent("Example 2",
				new TestNestedEmbeddedChild("Nested example", TestEnum.zero));
		testEntity.setNested(nested);

		final TestNestedEmbeddedParent firstNested = new TestNestedEmbeddedParent("Element 1",
				new TestNestedEmbeddedChild("Nested element 1", TestEnum.one));
		final TestNestedEmbeddedParent secondNested = new TestNestedEmbeddedParent("Element 2",
				new TestNestedEmbeddedChild("Nested element 1", TestEnum.two));
		testEntity.setManyNested(Arrays.asList(firstNested, secondNested));
		write(testEntity);

		final TestNestedEmbeddingEntity result = findSingleResult(
				"SELECT e FROM TestNestedEmbeddingEntity e WHERE e.id = 1", TestNestedEmbeddingEntity.class);
		assertThat(result.getId()).isEqualTo(testEntity.getId());

		final TestNestedEmbeddedParent resultNested = result.getNested();
		assertThat(resultNested.getDescription()).isEqualTo(nested.getDescription());
		assertThat(resultNested.getChild().getDescription()).isEqualTo(nested.getChild().getDescription());
		assertThat(resultNested.getChild().getTestEnum()).isEqualTo(nested.getChild().getTestEnum());

		final Collection<TestNestedEmbeddedParent> resultManyNested = result.getManyNested();
		assertThat(resultManyNested.stream().map(TestNestedEmbeddedParent::getDescription))
				.containsOnly(firstNested.getDescription(), secondNested.getDescription());
		final TestNestedEmbeddedParent resultFirstNested = resultManyNested.stream()
				.filter(e -> e.getDescription().equals(firstNested.getDescription())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException());
		assertThat(resultFirstNested.getChild().getDescription()).isEqualTo(firstNested.getChild().getDescription());
		assertThat(resultFirstNested.getChild().getTestEnum()).isEqualTo(firstNested.getChild().getTestEnum());
	}
}
