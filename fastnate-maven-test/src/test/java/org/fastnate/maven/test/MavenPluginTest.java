package org.fastnate.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that all entities were written to the SQL file during process classes.
 *
 * @author Tobias Liefke
 */
public class MavenPluginTest {

	private EntityManagerFactory emf;

	private EntityManager em;

	/**
	 * Build a entity manager factory (with a connected database) for testing.
	 */
	@Before
	public void setUp() {
		this.emf = Persistence.createEntityManagerFactory("test");
		this.em = this.emf.createEntityManager();
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
	 * Tests that the expected entities are available.
	 */
	@Test
	public void testEmbedded() {
		final CriteriaQuery<MavenTestEntity> criteriaQuery = this.em.getCriteriaBuilder()
				.createQuery(MavenTestEntity.class);
		criteriaQuery.select(criteriaQuery.from(MavenTestEntity.class));
		final List<MavenTestEntity> entities = this.em.createQuery(criteriaQuery).getResultList();
		final int prefixCount = 1;
		final int csvEntityCount = 2;
		final int mavenTestDataCount = 4;
		final int postfixCount = 1;
		assertThat(entities).hasSize(prefixCount + csvEntityCount + mavenTestDataCount + postfixCount);
	}

}
