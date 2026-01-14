package org.fastnate.generator.test;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a function that works useing a {@link Connection JDBC connection}.
 *
 * @author Tobias Liefke
 */
@FunctionalInterface
public interface SqlRunnable {

	/**
	 * Executes the work using the supplied connection.
	 *
	 * @param connection
	 *            The connection on which to perform the work.
	 *
	 * @throws SQLException
	 *             if there is a problem with the work
	 */
	void run(Connection connection) throws SQLException;

}
