package org.fastnate.generator.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import jakarta.persistence.EntityManager;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.test.ids.IdsTest;

/**
 * Provides methods for accessing functions specific for the current JPA implementation during the tests.
 *
 * @author Tobias Liefke
 */
public interface JpaProviderTestSetup {

	/**
	 * Execute some SQL statements using the current {@link java.sql.Connection JDBC connection}.
	 *
	 * @param em
	 *            the current entity manager
	 * @param work
	 *            The work to be performed.
	 *
	 * @throws SQLException
	 *             if there is a problem with the work
	 */
	default void executeSql(final EntityManager em, final SqlRunnable work) throws SQLException {
		work.run(getConnection(em));
	}

	/**
	 * Lookups the database connection in an entity manager.
	 *
	 * @param em
	 *            the current entity manager
	 * @return the database connection
	 * @throws SQLException
	 *             if the database connection is not available
	 */
	default Connection getConnection(final EntityManager em) throws SQLException {
		return em.unwrap(Connection.class);
	}

	/**
	 * Initializes the test setup.
	 *
	 * @param context
	 *            the current generator context
	 */
	default void initialize(final GeneratorContext context) {
		// The default does nothing
	}

	/**
	 * Initializes the properties for the JPA library and Fastnate.
	 *
	 * @param properties
	 *            the properties
	 */
	default void initialize(final Properties properties) {
		// The default does nothing
	}

	/**
	 * Indicates to test primitive IDs. Some JPA providers like Eclipse link have problems persisting entities with
	 * generated primitiv IDs.
	 *
	 * @return {code true} to run {@link IdsTest#testPrimitiveIds()}
	 */
	default boolean testPrimitiveIds() {
		return true;
	}

}
