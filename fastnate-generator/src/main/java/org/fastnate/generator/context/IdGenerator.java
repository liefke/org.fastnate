package org.fastnate.generator.context;

import java.io.IOException;

import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

/**
 * Saves the current value and increments the value for a {@link GeneratedIdProperty}.
 *
 * @author Tobias Liefke
 */
public abstract class IdGenerator {

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
	public abstract void addNextValue(TableStatement statement, GeneratorColumn column, Number nextValue);

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
	 * @return the generated value
	 */
	protected abstract long createNextValue();

	/**
	 * Resolves the next value of this generator.
	 *
	 * @param propertyClass
	 *            the type of the generated value
	 *
	 * @return the generated value
	 */
	public <N extends Number> N createNextValue(final Class<N> propertyClass) {
		final long nextValue = createNextValue();
		if (propertyClass == Long.class || propertyClass == long.class) {
			return (N) Long.valueOf(nextValue);
		}
		if (propertyClass == Integer.class || propertyClass == int.class) {
			return (N) Integer.valueOf((int) nextValue);
		}
		if (propertyClass == Short.class || propertyClass == short.class) {
			return (N) Short.valueOf((short) nextValue);
		}
		if (propertyClass == Byte.class || propertyClass == byte.class) {
			return (N) Byte.valueOf((byte) nextValue);
		}
		throw new ModelException("Can't handle number class for generated value: " + propertyClass);
	}

	/**
	 * Creates the statements that are needed in the output before
	 * {@link #addNextValue(TableStatement, GeneratorColumn, Number)}.
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
	public IdGenerator derive(final GeneratorTable entityTable) {
		return this;
	}

	/**
	 * The last value returned by {@link #createNextValue(Class)}.
	 *
	 * @return the current value
	 */
	public abstract long getCurrentValue();

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
	public abstract ColumnExpression getExpression(GeneratorTable table, GeneratorColumn column, Number id,
			boolean whereExpression);

	/**
	 * Indicates that {@link #createNextValue(Class)} should be called after the entity was written - as the value is
	 * not available before.
	 *
	 * @return {@code true} if the database increments the value _after_ the insert statement was executed, {@code true}
	 *         if it is incremented before or during the execution
	 */
	public abstract boolean isPostIncrement();

	/**
	 * Sets a new start value.
	 *
	 * @param currentValue
	 *            the current value - most likely as extracted from the target database
	 */
	public abstract void setCurrentValue(long currentValue);

}
