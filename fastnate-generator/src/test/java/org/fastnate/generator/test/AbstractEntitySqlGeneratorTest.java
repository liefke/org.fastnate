package org.fastnate.generator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;

import lombok.AccessLevel;
import lombok.Getter;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.dialect.H2Dialect;
import org.junit.After;
import org.junit.Before;

/**
 * Test the {@link EntitySqlGenerator} framework.
 *
 * @author Tobias Liefke
 */
public class AbstractEntitySqlGeneratorTest {

	private EntityManagerFactory emf;

	@Getter(AccessLevel.PROTECTED)
	private EntityManager em;

	@Getter(AccessLevel.PROTECTED)
	private EntitySqlGenerator generator;

	private static final int UNINTERESTING_FIELDS = Modifier.STATIC | Modifier.TRANSIENT;

	private static final List<Class<? extends Serializable>> PRIMITIVE_TYPES = Arrays.asList(String.class,
			Number.class, Boolean.class, Date.class);

	private <E> void assertCollectionsEquals(final Collection<E> writtenValue, final Collection<E> foundValue,
			final Collection<Object> inspected) {
		if (writtenValue == null || writtenValue.isEmpty()) {
			assertThat(foundValue).isNullOrEmpty();
		} else {
			assertThat(foundValue).hasSameSizeAs(writtenValue);
			for (final Iterator<E> writtenIt = writtenValue.iterator(), foundIt = foundValue.iterator(); writtenIt
					.hasNext();) {
				assertSingleValueEquals(writtenIt.next(), foundIt.next(), inspected);
			}
		}
	}

	/**
	 * Tests that a written entity is the same as a found one.
	 *
	 * @param written
	 *            the entity that was written using the EntitySqlGenerator
	 * @param found
	 *            the entity that was found in the EntityManager
	 */
	protected <E> void assertEntitiesEquals(final E written, final E found) {
		assertEntitiesEquals(written, found, new HashSet<Object>(Arrays.asList(written, found)));
	}

	private <E> void assertEntitiesEquals(final E written, final E found, final Collection<Object> inspected) {
		if (written == null) {
			assertThat(found).isNull();
		} else {
			assertThat(found).isNotNull();
			assertThat(found).isInstanceOf(written.getClass());
			if (inspected.add(written) || inspected.add(found)) {
				assertEntitiesEquals(written, found, inspected, written.getClass());
			}
		}
	}

	private <E> void assertEntitiesEquals(final E written, final E found, final Collection<Object> inspected,
			final Class<?> inspectedClass) {
		if (inspectedClass.getSuperclass() != null) {
			assertEntitiesEquals(written, found, inspected, inspectedClass.getSuperclass());
		}
		for (final Field field : inspectedClass.getDeclaredFields()) {
			if ((field.getModifiers() & UNINTERESTING_FIELDS) == 0) {
				assertFieldEquals(field, written, found, inspected);
			}
		}
	}

	private <K, E> void assertFieldEquals(final Field field, final E written, final E found,
			final Collection<Object> inspected) {
		try {
			field.setAccessible(true);
			final Object writtenValue = field.get(written);
			final Object foundValue = field.get(written);
			if (Collection.class.isAssignableFrom(field.getType())) {
				assertCollectionsEquals((Collection<E>) writtenValue, (Collection<E>) foundValue, inspected);
			} else if (Map.class.equals(foundValue)) {
				assertMapsEquals((Map<K, E>) writtenValue, (Map<K, E>) foundValue, inspected);
			} else {
				assertSingleValueEquals(written, found, inspected);
			}
		} catch (final IllegalArgumentException | IllegalAccessException e) {
			throw new AssertionError("Can't access field " + field, e);
		}
	}

	private <K, V> void assertMapsEquals(final Map<K, V> writtenValue, final Map<K, V> foundValue,
			final Collection<Object> inspected) {
		if (writtenValue == null || writtenValue.isEmpty()) {
			assertThat(foundValue).isNullOrEmpty();
		} else {
			assertThat(foundValue).hasSameSizeAs(writtenValue);
			for (final Map.Entry<K, V> entry : writtenValue.entrySet()) {
				assertSingleValueEquals(entry.getValue(), foundValue.get(entry.getKey()), inspected);
			}
		}
	}

	private <E> void assertSingleValueEquals(final E writtenValue, final E foundValue,
			final Collection<Object> inspected) {
		if (writtenValue == null) {
			assertThat(foundValue).isNull();
		} else {
			assertThat(foundValue).isNotNull();
			if (foundValue.getClass().isPrimitive()) {
				assertThat(foundValue).isEqualTo(writtenValue);
			} else {
				for (final Class<?> primitiveClass : PRIMITIVE_TYPES) {
					if (primitiveClass.isInstance(writtenValue)) {
						assertThat(foundValue).isEqualTo(writtenValue);
						return;
					}
				}
				assertEntitiesEquals(writtenValue, foundValue, inspected);
			}
		}
	}

	/**
	 * Finds all entities of the given entity class.
	 *
	 * @param entityClass
	 *            the class of the entity
	 * @return all entities of that class
	 */
	public <E> List<E> findResults(final Class<E> entityClass) {
		final CriteriaQuery<E> query = this.em.getCriteriaBuilder().createQuery(entityClass);
		query.select(query.from(entityClass));
		return this.em.createQuery(query).getResultList();
	}

	/**
	 * Finds all entities for the given query and entity class.
	 *
	 * @param query
	 *            the JPA-QL query
	 * @param entityClass
	 *            the class of the entity
	 * @return the result entities for the query
	 */
	public <E> List<E> findResults(final String query, final Class<E> entityClass) {
		return this.em.createQuery(query, entityClass).getResultList();
	}

	/**
	 * Finds the only entity for the given entity class.
	 *
	 * @param entityClass
	 *            the class of the entity
	 * @return the single entity of that class
	 */
	public <E> E findSingleResult(final Class<E> entityClass) {
		final CriteriaQuery<E> query = this.em.getCriteriaBuilder().createQuery(entityClass);
		query.select(query.from(entityClass));
		return this.em.createQuery(query).getSingleResult();
	}

	/**
	 * Finds the entity for the given query and entity class.
	 *
	 * @param query
	 *            the JPA-QL query
	 * @param entityClass
	 *            the class of the entity
	 * @return the single result entity for the query
	 */
	public <E> E findSingleResult(final String query, final Class<E> entityClass) {
		return this.em.createQuery(query, entityClass).getSingleResult();
	}

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
	 * Writes the entity with the generator.
	 *
	 * @param entity
	 *            the entity to write
	 * @throws IOException
	 *             if the generator throws one
	 */
	protected <E> void write(final E entity) throws IOException {
		this.generator.write(entity);
	}
}
