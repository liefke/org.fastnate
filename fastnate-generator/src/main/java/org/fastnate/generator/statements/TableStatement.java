package org.fastnate.generator.statements;

import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorTable;

/**
 * A SQL statement that affects a table and a set of columns.
 *
 * @author Tobias Liefke
 */
public interface TableStatement extends EntityStatement {

	/**
	 * The affected table of this update / insert statement.
	 *
	 * @return the metadata of the table
	 */
	GeneratorTable getTable();

	/**
	 * Sets a plain expression for a specific column.
	 *
	 * @param column
	 *            the metadata of the column
	 * @param value
	 *            the value expression
	 */
	void setColumnValue(GeneratorColumn column, ColumnExpression value);

}
