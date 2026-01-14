package org.fastnate.hibernate.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import jakarta.persistence.EntityManager;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.test.JpaProviderTestSetup;
import org.fastnate.generator.test.SqlRunnable;
import org.hibernate.Session;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.internal.SessionImpl;

/**
 * Initializes the tests when used with Hibernate.
 *
 * @author Tobias Liefke
 */
public class HibernateTestSetup implements JpaProviderTestSetup {

	@Override
	public void executeSql(final EntityManager em, final SqlRunnable work) {
		((Session) em.getDelegate()).doWork(work::run);
	}

	@Override
	public Connection getConnection(final EntityManager em) throws SQLException {
		return em.unwrap(SessionImpl.class).getJdbcConnectionAccess().obtainConnection();
	}

	@Override
	public void initialize(final GeneratorContext context) {
		if (!context.getDialect().isIdentitySupported()) {
			// Adjust entity manager to allow Identity in model
			context.getSettings().setProperty(JdbcSettings.DIALECT_RESOLVERS,
					AllowMissingIdentitySupportDialectResolver.class.getName());
		}
	}

	@Override
	public void initialize(final Properties properties) {
		properties.putIfAbsent("hibernate.show_sql", "true");
		properties.putIfAbsent("hibernate.format_sql", "false");
		properties.putIfAbsent("hibernate.use_sql_comments", "true");
	}

}