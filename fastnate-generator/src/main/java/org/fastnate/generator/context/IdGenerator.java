package org.fastnate.generator.context;

import java.util.List;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

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
	 *            the name of the ID column
	 * @param nextValue
	 *            the current value of the column, previously generated with {@link #createNextValue}
	 */
	public abstract void addNextValue(InsertStatement statement, String column, Number nextValue);

	/**
	 * Creates all statements that are necessary to set the next value created from the database is
	 * {@code currentValue + 1}.
	 *
	 * @return the statements - an empty list if nothing is necessary
	 */
	public abstract List<? extends EntityStatement> alignNextValue();

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
	 * {@link #addNextValue(InsertStatement, String, Number)}.
	 *
	 * @return statements that increase the sequence value before the insert statement
	 */
	public abstract List<? extends EntityStatement> createPreInsertStatements();

	/**
	 * Some implementations (like the Hibernate table generator) create a different generator, depending on the table
	 * name.
	 *
	 * @param table
	 *            the name of the current entity table
	 * @return the generator
	 */
	public IdGenerator derive(final String table) {
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
	 *            the name of the table of the entity
	 * @param column
	 *            the name of the column of the ID
	 * @param id
	 *            the current value of the ID
	 * @param whereExpression
	 *            indicates if this expression is needed for a "SELECT ... WHERE ..." - some dialects behave differently
	 *            in this situation
	 * @return the expression for selecting the ID
	 */
	public abstract String getExpression(String table, String column, Number id, boolean whereExpression);

	/**
	 * Indicates that {@link #createNextValue(Class)} should be called after the entity was written - as the value is
	 * not available before.
	 *
	 * @return {@code true} if the database increments the value _after_ the insert statement was executed, {@code true}
	 *         if it is incremented before or during the execution
	 */
	public abstract boolean isPostIncrement();

}
