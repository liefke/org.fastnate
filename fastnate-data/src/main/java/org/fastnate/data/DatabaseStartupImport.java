package org.fastnate.data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Imports data on startup of Hibernate, if {@link SessionFactoryOptions#getSchemaAutoTooling() schema auto tooling} is
 * set either to {@link SchemaAutoTooling#CREATE} or {@link SchemaAutoTooling#CREATE_DROP}.
 *
 * @author Tobias Liefke
 */
public class DatabaseStartupImport implements SessionFactoryObserver {

	private static final long serialVersionUID = 1L;

	@Override
	public void sessionFactoryClosed(final SessionFactory factory) {
		// Nothing to do
	}

	@Override
	public void sessionFactoryCreated(final SessionFactory factory) {
		// Only import, if Hibernate has just created an empty database
		final SchemaAutoTooling autoTooling = factory.getSessionFactoryOptions().getSchemaAutoTooling();
		if (autoTooling == SchemaAutoTooling.CREATE || autoTooling == SchemaAutoTooling.CREATE_DROP) {
			// Find a connection
			final SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
			try (Connection connection = sessionFactoryImplementor.getServiceRegistry()
					.getService(ConnectionProvider.class).getConnection()) {
				// Import data
				new EntityImporter(new Properties()).importData(connection);
			} catch (final SQLException | IOException e) {
				throw new RuntimeException("Could not import SQL data: " + e, e);
			}
		}
	}
}
