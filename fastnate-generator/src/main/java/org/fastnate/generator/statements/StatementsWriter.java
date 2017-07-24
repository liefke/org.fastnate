package org.fastnate.generator.statements;

import java.io.Closeable;
import java.io.IOException;

import org.fastnate.generator.context.DefaultContextModelListener;
import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.dialect.GeneratorDialect;

/**
 * Responsible to write {@link EntityStatement}s to a file or to the database.
 *
 * @author Tobias Liefke
 */
public interface StatementsWriter extends Closeable {

	/**
	 * Creates a container for a dedicated "insert into table" statement.
	 *
	 * @param dialect
	 *            the current database dialect
	 * @param table
	 *            the name of the table
	 * @return the created statement
	 */
	TableStatement createInsertStatement(GeneratorDialect dialect, GeneratorTable table);

	/**
	 * Creates a container for a plain SQL statement, which is just executed "as is".
	 *
	 * @param dialect
	 *            the current database dialect
	 * @param sql
	 *            the content of the sql statement (without any delimiter)
	 * @return the created statement
	 */
	EntityStatement createPlainStatement(GeneratorDialect dialect, String sql);

	/**
	 * Creates a container for a dedicated "update table" statement.
	 *
	 * @param dialect
	 *            the current database dialect
	 * @param table
	 *            the name of the affected table
	 * @param idColumn
	 *            the column that contains the id of the changed entity
	 * @param idValue
	 *            the expression for the id of the changed entity
	 * @return the created statement
	 */
	TableStatement createUpdateStatement(GeneratorDialect dialect, GeneratorTable table, GeneratorColumn idColumn,
			ColumnExpression idValue);

	/**
	 * Flushes any open statements.
	 *
	 * @throws IOException
	 *             if the target throws an exception
	 */
	void flush() throws IOException;

	/**
	 * Truncates all discovered tables before any data is written to that tables.
	 *
	 * @param context
	 *            the generator context
	 */
	default void truncateTables(final GeneratorContext context) {
		final DefaultContextModelListener truncater = new DefaultContextModelListener() {

			@Override
			public void foundTable(final GeneratorTable table) {
				try {
					context.getDialect().truncateTable(StatementsWriter.this, table);
				} catch (final IOException e) {
					throw new IllegalStateException(e);
				}
			}
		};
		for (final GeneratorTable table : context.getTables().values()) {
			truncater.foundTable(table);
		}
		context.addContextModelListener(truncater);
	}

	/**
	 * Writes a SQL comment to the target.
	 *
	 * @param comment
	 *            the comment to write
	 * @throws IOException
	 *             if the target throws an exception
	 */
	void writeComment(String comment) throws IOException;

	/**
	 * Writes a plain SQL statement.
	 *
	 * Shortcut for {@code writeStatement(createPlainStatement(sql), dialect)}.
	 *
	 * @param dialect
	 *            the current database dialect
	 * @param sql
	 *            the content of the SQL statement
	 * @throws IOException
	 *             if the file or database throws an exception
	 */
	default void writePlainStatement(final GeneratorDialect dialect, final String sql) throws IOException {
		writeStatement(createPlainStatement(dialect, sql));
	}

	/**
	 * Writes a new line to the target file to separate different sections in the SQL file.
	 *
	 * @throws IOException
	 *             if the target throws such an exception
	 */
	void writeSectionSeparator() throws IOException;

	/**
	 * Writes the given statement to a file or database.
	 *
	 * @param statement
	 *            contains the values to write
	 * @throws IOException
	 *             if the file or database throws an exception
	 */
	void writeStatement(EntityStatement statement) throws IOException;

}
