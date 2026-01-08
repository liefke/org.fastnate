package org.fastnate.generator.converter;

import java.lang.reflect.Constructor;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts a numeric value to a SQL expression.
 *
 * @author Tobias Liefke
 */
public class NumberConverter implements ValueConverter<Number> {

	private Constructor<? extends Number> stringConstructor;

	/**
	 * Creates a new instance of {@link NumberConverter}.
	 *
	 * @param type
	 *            the type of the number
	 * @throws ModelException
	 *             if the String constructor is missing from the given number class
	 */
	public NumberConverter(final Class<? extends Number> type) {
		try {
			this.stringConstructor = type.getConstructor(String.class);
		} catch (final NoSuchMethodException e) {
			throw new ModelException("Missing String constructor in " + type, e);
		}
	}

	@Override
	public ColumnExpression getExpression(final Number value, final GeneratorContext context) {
		return PrimitiveColumnExpression.create(value, context.getDialect());
	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		try {
			return PrimitiveColumnExpression.create(this.stringConstructor.newInstance(defaultValue),
					context.getDialect());
		} catch (final ReflectiveOperationException e) {
			throw new IllegalArgumentException("Can't convert default value '" + defaultValue + "' to "
					+ this.stringConstructor.getDeclaringClass(), e);
		}
	}

}
