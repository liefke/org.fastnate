package org.fastnate.generator.converter;

import org.fastnate.generator.DefaultValue;
import org.fastnate.generator.context.GeneratorContext;

/**
 * Converts a primitive value to an expression.
 *
 * @author Tobias Liefke
 * @param <T>
 *            The type of the handled values
 */
public interface ValueConverter<T> {

	/**
	 * Builds the expression for writing the default value into an SQL clause.
	 *
	 * @param defaultValue
	 *            the default value, as given in an {@link DefaultValue} expression
	 * @param context
	 *            the current context (contains the database dialect).
	 * @return the expression (that is including surrounding ' for string literals)
	 */
	String getExpression(String defaultValue, GeneratorContext context);

	/**
	 * Builds the expression for writing the given value into an SQL clause.
	 *
	 * @param value
	 *            the current value
	 * @param context
	 *            the current context (contains the database dialect).
	 * @return the expression (that is including surrounding ' for string literals)
	 */
	String getExpression(T value, GeneratorContext context);

}
