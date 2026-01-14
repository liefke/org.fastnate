package org.fastnate.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A class that is implemented by the fastnate-data module and is used by the actual JPA implementation to start the
 * data import as soon as startup of JPA has finished.
 *
 * @author Tobias Liefke
 */
public interface StartupImport {

	/**
	 * Start import of all the data.
	 *
	 * @param connection
	 *            the current database connection
	 * @throws SQLException
	 *             if there was a problem when accessing the database connection
	 * @throws IOException
	 *             if there was a prolbem when reading the data
	 */
	void importData(Connection connection) throws IOException, SQLException;

}
