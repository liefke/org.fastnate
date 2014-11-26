package org.fastnate.generator.converter;

import org.fastnate.generator.context.GeneratorContext;

/**
 * Converts a boolean to a SQL expression.
 *
 * @author Tobias Liefke
 */
public class BooleanConverter extends AbstractValueConverter<Boolean> {

	@Override
	public String getExpression(final Boolean value, final GeneratorContext context) {
		return context.getDialect().convertBooleanValue(Boolean.TRUE.equals(value));
	}

	@Override
	public String getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression(defaultValue.equals("true") || defaultValue.equals("1"), context);
	}

}
