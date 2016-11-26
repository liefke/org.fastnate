package org.fastnate.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaQuery;

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
		final int csvEntityCount = 2;
		final MavenTestData mavenTestData = new MavenTestData(new File("src/main/data"));
		mavenTestData.buildEntities();
		assertThat(entities).hasSize(csvEntityCount + mavenTestData.getEntities().size());
	}

}
