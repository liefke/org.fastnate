package org.fastnate.hibernate.data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ServiceLoader;

import jakarta.persistence.PersistenceException;

import org.fastnate.util.StartupImport;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Imports data on startup of Hibernate, if the database was just created.
 *
 * @author Tobias Liefke
 */
public class DatabaseStartupImport implements SessionFactoryObserver {

	private static final long serialVersionUID = 1L;

	@Override
	public void sessionFactoryCreated(final SessionFactory factory) {
		// Only create connection, if there is indeed fastnate-data on the classpath
		final Iterator<StartupImport> startupImports = ServiceLoader.load(StartupImport.class).iterator();

		if (startupImports.hasNext()) {
			// Only import, if the JPA provider has just created an empty database
			final String databaseAction = String
					.valueOf(factory.getProperties().get("jakarta.persistence.schema-generation.database.action"))
					.toUpperCase();
			if (databaseAction.contains("CREATE")) {
				// Find a connection
				final SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
				try (Connection connection = sessionFactoryImplementor.getServiceRegistry()
						.getService(ConnectionProvider.class).getConnection()) {
					// Import data
					while (startupImports.hasNext()) {
						startupImports.next().importData(connection);
					}
				} catch (final SQLException | IOException e) {
					throw new PersistenceException("Could not import SQL data: " + e, e);
				}
			}
		}
	}
}
