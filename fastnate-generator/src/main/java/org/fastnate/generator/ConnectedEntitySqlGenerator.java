package org.fastnate.generator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.EntityStatement;

import lombok.Getter;

/**
 * An extenson of an {@link EntitySqlGenerator} which writes the SQL statements directly to a database connection.
 *
 * @author Tobias Liefke
 */
public class ConnectedEntitySqlGenerator extends EntitySqlGenerator {

	private final Statement statement;

	@Getter
	private final Connection connection;

	/**
	 * Creates a new generator that writes to a database connection.
	 *
	 * @param connection
	 *            the database connection
	 * @throws SQLException
	 *             if the connection is invalid
	 */
	public ConnectedEntitySqlGenerator(final Connection connection) throws SQLException {
		this(connection, new GeneratorContext());
	}

	/**
	 * Creates a new generator that writes to a database connection.
	 *
	 * @param connection
	 *            the database connection
	 * @param context
	 *            used to keep the state of indices and to store any configuration.
	 * @throws SQLException
	 *             if the connection is invalid
	 */
	public ConnectedEntitySqlGenerator(final Connection connection, final GeneratorContext context)
			throws SQLException {
		super(context);
		this.connection = connection;
		this.statement = connection.createStatement();
	}

	@Override
	public void close() throws IOException {
		super.close();
		try {
			this.statement.close();
		} catch (final SQLException e) {
			// Ignore - as this will only happen, if there was already an exception during update
		}
	}

	@Override
	public void writeComment(final String comment) throws IOException {
		// Ignore, as the database is not interested in comments
	}

	@Override
	public void writeSectionSeparator() throws IOException {
		// Ignore, as nothing happens in the database
	}

	@Override
	public void writeStatement(final EntityStatement stmt) throws IOException {
		String sql = getContext().getDialect().createSql(stmt);
		if (sql.endsWith(";\n")) {
			sql = sql.substring(0, sql.length() - 2);
		}
		try {
			this.statement.executeUpdate(sql);
		} catch (final SQLException e) {
			throw new IOException("Could not execute statement: " + sql, e);
		}
	}

}
