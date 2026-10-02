package org.fastnate.generator.context;

import java.io.IOException;
import java.io.Serializable;

import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

/**
 * Generates the next value for a {@link GeneratedIdProperty}.
 *
 * @author Tobias Liefke
 * @param <V>
 *            the type of the generated values
 */
public abstract class IdGenerator<V extends Serializable> {

	/**
	 * Adds the generated value to the given statement.
	 *
	 * Depending on the generator this may add the value itself or a reference to the value.
	 *
	 * @param statement
	 *            the current insert statement
	 * @param column
	 *            the ID column of the current table
	 * @param nextValue
	 *            the current value of the column, previously generated with {@link #createNextValue}
	 */
	public abstract void addNextValue(TableStatement statement, GeneratorColumn column, V nextValue);

	/**
	 * Creates all statements that are necessary to set the next value created from the database is
	 * {@code currentValue + 1}.
	 *
	 * @param writer
	 *            the target of the created statements
	 * @throws IOException
	 *             if the writer throws one
	 */
	public abstract void alignNextValue(StatementsWriter writer) throws IOException;

	/**
	 * Resolves the next value of this generator.
	 *
	 * @param propertyClass
	 *            the type of the generated value
	 *
	 * @return the generated value
	 */
	public abstract V createNextValue(Class<V> propertyClass);

	/**
	 * Creates the statements that are needed in the output before {@link #addNextValue}.
	 *
	 * @param writer
	 *            target for the created statements
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void createPreInsertStatements(final StatementsWriter writer) throws IOException {
		// The default does nothing
	}

	/**
	 * Some implementations (like the Hibernate table generator) create a different generator, depending on the table
	 * name.
	 *
	 * @param entityTable
	 *            the current entity table
	 * @return the generator
	 */
	public IdGenerator<V> derive(final GeneratorTable entityTable) {
		return this;
	}

	/**
	 * Builds the reference to another entity that has the given ID.
	 *
	 * @param table
	 *            the main table of the entity
	 * @param column
	 *            the name of the column of the ID
	 * @param id
	 *            the current value of the ID
	 * @param whereExpression
	 *            indicates if this expression is needed for a "SELECT ... WHERE ..." - some dialects behave differently
	 *            in this situation
	 * @return the expression for selecting the ID
	 */
	public abstract ColumnExpression getExpression(GeneratorTable table, GeneratorColumn column, V id,
			boolean whereExpression);

	/**
	 * Indicates that {@link #createNextValue} should be called after the entity was written - as the value is not
	 * available before.
	 *
	 * @return {@code true} if the database increments the value _after_ the insert statement was executed, {@code true}
	 *         if it is incremented before or during the execution
	 */
	public abstract boolean isPostIncrement();

}
