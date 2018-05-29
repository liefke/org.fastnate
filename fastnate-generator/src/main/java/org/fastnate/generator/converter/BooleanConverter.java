package org.fastnate.generator.converter;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts a boolean to a SQL expression.
 *
 * @author Tobias Liefke
 */
public class BooleanConverter implements ValueConverter<Boolean> {

	@Override
	public ColumnExpression getExpression(final Boolean value, final GeneratorContext context) {
		return new PrimitiveColumnExpression<>(value, context.getDialect()::convertBooleanValue);
	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression(defaultValue.equals("true") || defaultValue.equals("1"), context);
	}

}
