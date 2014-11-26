package org.fastnate.generator.converter;

import org.fastnate.generator.context.GeneratorContext;

/**
 * Converts a numeric value to a SQL expression.
 * 
 * @author Tobias Liefke
 */
public class NumberConverter extends AbstractValueConverter<Number> {

	@Override
	public String getExpression(final Number value, final GeneratorContext context) {
		return String.valueOf(value);
	}
}
