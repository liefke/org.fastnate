package org.fastnate.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.fastnate.generator.testmodel.PrimitiveTestEntity;
import org.fastnate.generator.testmodel.TestPluralEntity;
import org.fastnate.generator.testmodel.TestPluralEntityProperty;
import org.junit.Test;

/**
 * Test collections in entities.
 *
 * @author Tobias Liefke
 */
public class CollectionsTest extends AbstractEntitySqlGeneratorTest {

	private static List<String> extractNames(final Iterable<PrimitiveTestEntity> entities) {
		final List<String> names = new ArrayList<>();
		for (final PrimitiveTestEntity entity : entities) {
			names.add(entity.getName());
		}
		return names;
	}

	/**
	 * Tests to write plural properties and fixed id.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testPlural() throws IOException {
		final TestPluralEntity testEntity = new TestPluralEntity();
		testEntity.setId(1L);
		testEntity.getStringSet().add("Test1");
		testEntity.getStringSet().add("Test2");

		final TestPluralEntity otherEntity = new TestPluralEntity();
		otherEntity.setId(2L);
		otherEntity.getStringSet().add("Test2");
		otherEntity.getStringSet().add("Test3");

		testEntity.getStringList().add("list1");
		testEntity.getStringList().add("list2");

		testEntity.getOrderedStringList().add("A");
		testEntity.getOrderedStringList().add("Z");
		testEntity.getOrderedStringList().add("M");

		testEntity.getEmbeddedList().add(new TestPluralEntityProperty("Test desc", otherEntity));
		testEntity.getEmbeddedList().add(new TestPluralEntityProperty("Test desc 2", otherEntity));

		final PrimitiveTestEntity testChild1 = new PrimitiveTestEntity("Plural test child 1");
		final PrimitiveTestEntity testChild2 = new PrimitiveTestEntity("Plural test child 2");
		testEntity.getEntitySet().add(testChild1);
		testEntity.getEntitySet().add(testChild2);

		final PrimitiveTestEntity testChild3 = new PrimitiveTestEntity("Plural test child 3");
		testEntity.getEntityList().add(testChild1);
		testEntity.getEntityList().add(testChild3);

		testEntity.getOrderedEntityList().add(new PrimitiveTestEntity("Plural sort test 1"));
		testEntity.getOrderedEntityList().add(new PrimitiveTestEntity("Plural sort test 3"));
		testEntity.getOrderedEntityList().add(new PrimitiveTestEntity("Plural sort test 2"));

		write(testEntity);

		final TestPluralEntity result = findSingleResult(
				"SELECT e FROM TestPluralEntity e JOIN e.stringSet s WHERE s = 'Test1'", TestPluralEntity.class);

		assertThat(result.getStringSet()).containsOnly("Test1", "Test2");
		assertThat(result.getStringList()).containsOnly("list1", "list2");
		assertThat(result.getOrderedStringList()).containsExactly("A", "Z", "M");

		assertThat(result.getEmbeddedList()).hasSize(2);
		assertThat(result.getEmbeddedList().get(0).getDescription()).isEqualTo(
				testEntity.getEmbeddedList().get(0).getDescription());
		assertThat(result.getEmbeddedList().get(0).getOtherEntity().getStringSet()).containsOnly("Test2", "Test3");
		assertThat(result.getEmbeddedList().get(1).getDescription()).isEqualTo(
				testEntity.getEmbeddedList().get(1).getDescription());
		assertThat(result.getEmbeddedList().get(1).getOtherEntity().getId()).isEqualTo(
				testEntity.getEmbeddedList().get(0).getOtherEntity().getId());

		assertThat(result.getEntitySet()).hasSize(2);
		assertThat(extractNames(result.getEntitySet())).containsOnly(testChild1.getName(), testChild2.getName());

		assertThat(result.getEntityList()).hasSize(2);
		assertThat(extractNames(result.getEntityList())).containsOnly(testChild1.getName(), testChild3.getName());

		assertThat(result.getOrderedEntityList()).hasSize(testEntity.getOrderedEntityList().size());
		assertThat(extractNames(result.getOrderedEntityList())).containsExactly("Plural sort test 1",
				"Plural sort test 3", "Plural sort test 2");
	}

}
