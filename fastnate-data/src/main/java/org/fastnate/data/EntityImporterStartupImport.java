package org.fastnate.data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.fastnate.util.StartupImport;

/**
 * Used by the JPA provider to import data as soon as the database is created.
 *
 * @author Tobias Liefke
 */
public class EntityImporterStartupImport implements StartupImport {

	@Override
	public void importData(final Connection connection) throws IOException, SQLException {
		new EntityImporter(new Properties()).importData(connection);
	}

}
