package org.fastnate.generator.test;

import java.io.IOException;
import java.util.List;

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
