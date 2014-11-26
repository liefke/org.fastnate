package org.fastnate.generator;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.lang.time.DateUtils;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.dialect.H2Dialect;
import org.fastnate.generator.testmodel.TestEmbeddedId;
import org.fastnate.generator.testmodel.TestEmbeddedProperties;
import org.fastnate.generator.testmodel.TestEmbeddingEntity;
import org.fastnate.generator.testmodel.TestPluralEntity;
import org.fastnate.generator.testmodel.TestPluralEntityProperty;
import org.fastnate.generator.testmodel.TestRecursiveEntity;
import org.fastnate.generator.testmodel.TestRequiredEmbeddedProperties;
import org.fastnate.generator.testmodel.TestSingularEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link EntitySqlGenerator} framework.
 *
 * @author Tobias Liefke
 */
public class EntitySqlGeneratorTest {

	private static List<String> extractNames(final Iterable<TestSingularEntity> entities) {
		final List<String> names = new ArrayList<>();
		for (final TestSingularEntity entity : entities) {
			names.add(entity.getName());
		}
		return names;
	}

	private EntityManagerFactory emf;

	private EntityManager em;

	private EntitySqlGenerator generator;

	/**
	 * Build a entity manager factory (with a connected database) for testing.
	 */
	@Before
	public void setUp() {
		this.emf = Persistence.createEntityManagerFactory("test");
		this.em = this.emf.createEntityManager();
		@SuppressWarnings("resource")
		final SqlEmWriter sqlWriter = new SqlEmWriter(this.em);
		final GeneratorContext context = new GeneratorContext(new H2Dialect());
		context.setMaxUniqueProperties(0);
		this.generator = new EntitySqlGenerator(sqlWriter, context);
	}

	/**
	 * Close the entity manager factory.
	 */
	@After
	public void tearDown() {
		this.em.close();
		this.emf.close();
	}

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
		final TestEmbeddedProperties testProperties = new TestEmbeddedProperties("Example", new TestSingularEntity(
				"embedded child"));
		testEntity.setProperties(testProperties);
		testEntity.setRequired(new TestRequiredEmbeddedProperties("req", "opt"));
		this.generator.write(testEntity);

		final TestEmbeddingEntity result = this.em.createQuery("SELECT e FROM EmbedEnty e WHERE e.id.id = 2",
				TestEmbeddingEntity.class).getSingleResult();
		assertThat(result.getId()).isEqualTo(testEntity.getId());
		final TestEmbeddedProperties resultProperties = result.getProperties();
		assertThat(resultProperties.getDescription()).isEqualTo(testProperties.getDescription());
		assertThat(resultProperties.getOtherNode().getName()).isEqualTo(testProperties.getOtherNode().getName());

		assertThat(result.getRequired().getRequired()).isEqualTo(testEntity.getRequired().getRequired());
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

		final TestSingularEntity testChild1 = new TestSingularEntity("Plural test child 1");
		final TestSingularEntity testChild2 = new TestSingularEntity("Plural test child 2");
		testEntity.getEntitySet().add(testChild1);
		testEntity.getEntitySet().add(testChild2);

		final TestSingularEntity testChild3 = new TestSingularEntity("Plural test child 3");
		testEntity.getEntityList().add(testChild1);
		testEntity.getEntityList().add(testChild3);

		testEntity.getOrderedEntityList().add(new TestSingularEntity("Plural sort test 1"));
		testEntity.getOrderedEntityList().add(new TestSingularEntity("Plural sort test 3"));
		testEntity.getOrderedEntityList().add(new TestSingularEntity("Plural sort test 2"));

		this.generator.write(testEntity);

		final TestPluralEntity result = this.em.createQuery(
				"SELECT e FROM TestPluralEntity e JOIN e.stringSet s WHERE s = 'Test1'", TestPluralEntity.class)
				.getSingleResult();

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

	/**
	 * Tests to write primitive properties in an entity.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testPrimitiveProperties() throws IOException {
		final TestSingularEntity testEntity = new TestSingularEntity("Test Primitives");

		testEntity.setDescription("Test all primitive values and 'escaping'.\n\t\r\b\"New line\".\'");
		testEntity.setTestChar('\'');

		testEntity.setTestBoolean(true);

		testEntity.setTestByte(Byte.MIN_VALUE);
		testEntity.setTestShort(Short.MIN_VALUE);
		testEntity.setTestInt(Integer.MIN_VALUE);
		testEntity.setTestLong(Long.MIN_VALUE);

		final float testFloat = -0.987654321f;
		testEntity.setTestFloat(testFloat);
		final double testDouble = -0.98765432109876543210;
		testEntity.setTestDouble(testDouble);

		final long oneHour = DateUtils.MILLIS_PER_HOUR;
		final long oneDay = DateUtils.MILLIS_PER_DAY;
		final long time = oneDay + oneHour;
		testEntity.setTestDate(new Date(time));
		testEntity.setTestTime(new Date(time));
		testEntity.setTestTimestamp(new Date(time));

		testEntity.setTransient1("transient 1");
		testEntity.setTransient2("transient 2");

		testEntity.setManyCharacters("Many \r\n Characters".toCharArray());
		testEntity.setManyBytes("\0\1\2\3\4\5\6\7\t\r\n\b\f\u0027 Bytes".getBytes("ISO-8859-1"));

		this.generator.write(testEntity);

		// Test equalness
		final TestSingularEntity result = this.em
				.createNamedQuery(TestSingularEntity.NQ_ENTITY_BY_NAME, TestSingularEntity.class)
				.setParameter("name", testEntity.getName()).getSingleResult();
		assertThat(result.getName()).isEqualTo(testEntity.getName());
		assertThat(result.getDescription()).isEqualTo(testEntity.getDescription());
		assertThat(result.getTestChar()).isEqualTo(testEntity.getTestChar());
		assertThat(result.isTestBoolean()).isEqualTo(testEntity.isTestBoolean());
		assertThat(result.getTestByte()).isEqualTo(testEntity.getTestByte());
		assertThat(result.getTestShort()).isEqualTo(testEntity.getTestShort());
		assertThat(result.getTestInt()).isEqualTo(testEntity.getTestInt());
		assertThat(result.getTestLong()).isEqualTo(testEntity.getTestLong());
		assertThat(result.getTestFloat()).isEqualTo(testEntity.getTestFloat());
		assertThat(result.getTestDouble()).isEqualTo(testEntity.getTestDouble());

		assertThat(result.getManyCharacters()).isEqualTo(testEntity.getManyCharacters());
		assertThat(result.getManyBytes()).isEqualTo(testEntity.getManyBytes());

		// Check that only the parts of the date are written
		assertThat(result.getTestDate()).isEqualTo(DateUtils.truncate(testEntity.getTestDate(), Calendar.DATE));
		assertThat(result.getTestTime()).isEqualTo(new Date(oneHour));
		// Ignore the millis for timestamp comparison
		assertThat(
				new Date(result.getTestTimestamp().getTime() - result.getTestTimestamp().getTime()
						% DateUtils.MILLIS_PER_SECOND)).isEqualTo(testEntity.getTestTimestamp());

		// Check that no transient fields are written
		assertThat(result.getTransient1()).isNull();
		assertThat(result.getTransient2()).isNull();
	}

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
		this.generator.write(root);

		final TestRecursiveEntity writtenChild = this.em
				.createNamedQuery(TestRecursiveEntity.NQ_ENTITY_BY_NAME, TestRecursiveEntity.class)
				.setParameter("name", child.getName()).getSingleResult();
		assertThat(writtenChild.getChildren()).isEmpty();
		final TestRecursiveEntity writtenRoot = writtenChild.getParent();
		assertThat(writtenRoot.getName()).isEqualTo(root.getName());
		assertThat(writtenRoot.getParent()).isNull();
		assertThat(writtenRoot.getChildren()).contains(writtenChild);

		final TestRecursiveEntity root2 = new TestRecursiveEntity(null, "Test Recursion Root2");
		final TestRecursiveEntity child2 = new TestRecursiveEntity(root2, "Test Recursion Child2");
		this.generator.write(child2);

		final TestRecursiveEntity writtenRoot2 = this.em
				.createNamedQuery(TestRecursiveEntity.NQ_ENTITY_BY_NAME, TestRecursiveEntity.class)
				.setParameter("name", root2.getName()).getSingleResult();
		assertThat(writtenRoot2.getParent()).isNull();
		assertThat(writtenRoot2.getChildren()).hasSize(1);
		final TestRecursiveEntity writtenChild2 = writtenRoot2.getChildren().iterator().next();
		assertThat(writtenChild2.getName()).isEqualTo(child2.getName());
	}

}
