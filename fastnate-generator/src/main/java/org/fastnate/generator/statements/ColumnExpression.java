package org.fastnate.generator.statements;

/**
 * An expression for a column value in an {@link TableStatement}.
 *
 * @author Tobias Liefke
 */
public interface ColumnExpression {

	/**
	 * Adds the SQL for this expression to the given {@link StringBuilder}.
	 *
	 * @param statement
	 *            contains the SQL for the whole statement
	 */
	default void appendSql(final StringBuilder statement) {
		statement.append(toSql());
	}

	/**
	 * Generates the SQL for this expression.
	 *
	 * @return the SQL string
	 */
	String toSql();

}
