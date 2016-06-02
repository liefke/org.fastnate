package org.fastnate.generator.test.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.fastnate.generator.test.SimpleTestEntity;
import org.fastnate.generator.test.primitive.TestEnum;
import org.junit.Test;

/**
 * Test collections in entities.
 *
 * @author Tobias Liefke
 */
public class CollectionsTest extends AbstractEntitySqlGeneratorTest {

	private static List<String> extractNames(final Iterable<SimpleTestEntity> entities) {
		final List<String> names = new ArrayList<>();
		for (final SimpleTestEntity entity : entities) {
			names.add(entity.getName());
		}
		return names;
	}

	/**
	 * Tests to write plural properties.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testPlural() throws IOException {
		final CollectionsTestEntity testEntity = new CollectionsTestEntity();
		testEntity.getStringSet().add("Test1");
		testEntity.getStringSet().add("Test2");

		final CollectionsTestEntity otherEntity = new CollectionsTestEntity();
		otherEntity.getStringSet().add("Test2");
		otherEntity.getStringSet().add("Test3");

		testEntity.getEnumList().add(TestEnum.two);
		testEntity.getEnumList().add(TestEnum.one);

		testEntity.getStringList().add("list1");
		testEntity.getStringList().add("list2");

		testEntity.getOrderedStringList().add("A");
		testEntity.getOrderedStringList().add("Z");
		testEntity.getOrderedStringList().add("M");

		testEntity.getEmbeddedList().add(new CollectionsTestEntityProperty("Test desc", otherEntity));
		testEntity.getEmbeddedList().add(new CollectionsTestEntityProperty("Test desc 2", otherEntity));

		final SimpleTestEntity testChild1 = new SimpleTestEntity("Plural test child 1");
		final SimpleTestEntity testChild2 = new SimpleTestEntity("Plural test child 2");
		testEntity.getEntitySet().add(testChild1);
		testEntity.getEntitySet().add(testChild2);

		final SimpleTestEntity testChild3 = new SimpleTestEntity("Plural test child 3");
		testEntity.getEntityList().add(testChild1);
		testEntity.getEntityList().add(testChild3);

		testEntity.getOrderedEntityList().add(new SimpleTestEntity("Plural sort test 1"));
		testEntity.getOrderedEntityList().add(new SimpleTestEntity("Plural sort test 3"));
		testEntity.getOrderedEntityList().add(new SimpleTestEntity("Plural sort test 2"));

		testEntity.getChildren().add(new ChildTestEntity(testEntity, "First child"));
		testEntity.getChildren().add(new ChildTestEntity(testEntity, "Second child"));
		write(testEntity);

		final CollectionsTestEntity result = findSingleResult(
				"SELECT e FROM CollectionsTestEntity e JOIN e.stringSet s WHERE s = 'Test1'",
				CollectionsTestEntity.class);

		assertThat(result.getStringSet()).containsOnly("Test1", "Test2");
		assertThat(result.getStringList()).containsOnly("list1", "list2");
		assertThat(result.getOrderedStringList()).containsExactly("A", "Z", "M");

		assertThat(result.getEnumList()).containsExactly(TestEnum.one, TestEnum.two);

		assertThat(result.getEmbeddedList()).hasSize(2);
		assertThat(result.getEmbeddedList().get(0).getDescription())
				.isEqualTo(testEntity.getEmbeddedList().get(0).getDescription());
		assertThat(result.getEmbeddedList().get(0).getOtherEntity().getStringSet()).containsOnly("Test2", "Test3");
		assertThat(result.getEmbeddedList().get(1).getDescription())
				.isEqualTo(testEntity.getEmbeddedList().get(1).getDescription());

		assertThat(result.getEntitySet()).hasSize(2);
		assertThat(extractNames(result.getEntitySet())).containsOnly(testChild1.getName(), testChild2.getName());

		assertThat(result.getEntityList()).hasSize(2);
		assertThat(extractNames(result.getEntityList())).containsOnly(testChild1.getName(), testChild3.getName());

		assertThat(result.getOrderedEntityList()).hasSize(testEntity.getOrderedEntityList().size());
		assertThat(extractNames(result.getOrderedEntityList())).containsExactly("Plural sort test 1",
				"Plural sort test 3", "Plural sort test 2");

		assertThat(result.getChildren()).hasSize(2);
		assertThat(result.getChildren().get(0).getName()).isEqualTo("First child");
		assertThat(result.getChildren().get(1).getName()).isEqualTo("Second child");
	}

}
