package org.fastnate.generator.test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.GeneratorContext;
import org.junit.After;
import org.junit.Before;

import lombok.AccessLevel;
import lombok.Getter;

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
		final Properties properties = new Properties(System.getProperties());
		this.emf = Persistence
				.createEntityManagerFactory(properties.getProperty(GeneratorContext.PERSISTENCE_UNIT_KEY, "test-h2"));
		this.em = this.emf.createEntityManager();
		@SuppressWarnings("resource")
		final SqlEmWriter sqlWriter = new SqlEmWriter(this.em);
		final GeneratorContext context = new GeneratorContext(properties);
		context.setMaxUniqueProperties(0);
		this.generator = new EntitySqlGenerator(sqlWriter, context);
	}

	/**
	 * Close the entity manager factory.
	 */
	@After
	public void tearDown() {
		if (this.em != null) {
			this.em.close();
		}
		if (this.emf != null) {
			this.emf.close();
		}
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
