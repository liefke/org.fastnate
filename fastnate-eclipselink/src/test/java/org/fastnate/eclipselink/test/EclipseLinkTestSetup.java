package org.fastnate.eclipselink.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import jakarta.persistence.EntityManager;

import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sessions.Session;
import org.fastnate.generator.test.JpaProviderTestSetup;

/**
 * Initializes the tests when used with Eclipse.
 *
 * @author Tobias Liefke
 */
public class EclipseLinkTestSetup implements JpaProviderTestSetup {

	@Override
	public Connection getConnection(final EntityManager em) throws SQLException {
		return ((AbstractSession) em.unwrap(Session.class)).getAccessor().getConnection();
	}

	@Override
	public void initialize(final Properties properties) {
		properties.setProperty("eclipselink.weaving", "false");
		properties.setProperty("eclipselink.logging.level", "FINE");
	}

	@Override
	public boolean testPrimitiveIds() {
		return false;
	}

}