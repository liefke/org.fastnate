package org.fastnate.generator.test.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Convert;

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
	 * Tests to write embedded collections and prevent circular dependencies.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testEmbedded() throws IOException {
		final CollectionsTestEntity testEntity = new CollectionsTestEntity();
		testEntity.getEnumList().add(TestEnum.one);
		final CollectionsTestEntity testEntity2 = new CollectionsTestEntity();
		testEntity2.getElements().add(new CollectionsTestElement(testEntity));
		testEntity.getElements().add(new CollectionsTestElement(testEntity2));
		testEntity.setOther(testEntity2);

		write(testEntity);

		final CollectionsTestEntity result = findSingleResult("SELECT e FROM CTE e JOIN e.enumList en WHERE en = 1",
				CollectionsTestEntity.class);
		assertThat(result.getElements()).hasSize(1);
		final CollectionsTestEntity target = result.getElements().iterator().next().getEntity();
		assertThat(target).isNotEqualTo(result);
		assertThat(target.getElements()).hasSize(1);
		assertThat(target.getElements().iterator().next().getEntity()).isEqualTo(result);
	}

	/**
	 * Tests to write {@link Convert}.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testListConverter() throws IOException {
		final CollectionsTestEntity testEntity = new CollectionsTestEntity();
		testEntity.getConvertedStrings().addAll(Arrays.asList("abc", "ab", "bc", "de"));
		write(testEntity);

		final CollectionsTestEntity testEntity2 = new CollectionsTestEntity();
		testEntity2.getConvertedStrings().add("abcdef");
		write(testEntity2);

		final CollectionsTestEntity result = findSingleResult(
				"SELECT e FROM CTE e WHERE concat(',', e.convertedStrings, ',') LIKE concat('%,', :item, ',%')",
				CollectionsTestEntity.class, "item", "de");

		assertThat(result.getConvertedStrings()).containsExactlyElementsOf(testEntity.getConvertedStrings());
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
		testEntity.getStringSet().addAll(Arrays.asList("Test1", "Test2"));

		final CollectionsTestEntity otherEntity = new CollectionsTestEntity();
		otherEntity.getStringSet().addAll(Arrays.asList("Test2", "Test3"));

		testEntity.getEnumList().addAll(Arrays.asList(TestEnum.two, TestEnum.one));

		testEntity.getStringList().addAll(Arrays.asList("list1", "list2"));

		testEntity.getOrderedStringList().addAll(Arrays.asList("A", "Z", "M"));

		testEntity.getEmbeddedList().add(new CollectionsTestEntityProperty("Test desc", otherEntity));
		testEntity.getEmbeddedList().add(new CollectionsTestEntityProperty("Test desc 2", otherEntity));

		final SimpleTestEntity testChild1 = new SimpleTestEntity("Plural test child 1");
		final SimpleTestEntity testChild2 = new SimpleTestEntity("Plural test child 2");
		testEntity.getEntitySet().addAll(Arrays.asList(testChild1, testChild2));

		final SimpleTestEntity testChild3 = new SimpleTestEntity("Plural test child 3");
		testEntity.getEntityList().addAll(Arrays.asList(testChild1, testChild3));

		testEntity.getOrderedEntityList().add(new SimpleTestEntity("Plural sort test 1"));
		testEntity.getOrderedEntityList().add(new SimpleTestEntity("Plural sort test 3"));
		testEntity.getOrderedEntityList().add(new SimpleTestEntity("Plural sort test 2"));

		testEntity.getChildren().add(new ChildTestEntity(testEntity, "First child"));
		testEntity.getChildren().add(new ChildTestEntity(testEntity, "Second child"));

		testEntity.getChildren().get(0).getOtherParents().add(otherEntity);
		otherEntity.getOtherChildren().add(testEntity.getChildren().get(0));
		write(testEntity);

		final CollectionsTestEntity result = findSingleResult(
				"SELECT e FROM CTE e JOIN e.stringSet s WHERE s = 'Test1'", CollectionsTestEntity.class);

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
		final ChildTestEntity firstChild = result.getChildren().get(0);
		assertThat(firstChild.getName()).isEqualTo("First child");
		assertThat(result.getChildren().get(1).getName()).isEqualTo("Second child");

		final Set<CollectionsTestEntity> otherParents = firstChild.getOtherParents();
		assertThat(otherParents).hasSize(1);
		assertThat(otherParents.iterator().next()).isEqualTo(result.getEmbeddedList().get(0).getOtherEntity());
		assertThat(otherParents.iterator().next().getOtherChildren()).containsOnly(firstChild);
	}

}
