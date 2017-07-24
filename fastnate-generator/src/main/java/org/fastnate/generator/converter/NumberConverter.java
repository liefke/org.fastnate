package org.fastnate.generator.converter;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts a numeric value to a SQL expression.
 *
 * @author Tobias Liefke
 */
public class NumberConverter extends AbstractValueConverter<Number> {

	@Override
	public ColumnExpression getExpression(final Number value, final GeneratorContext context) {
		return PrimitiveColumnExpression.create(value, context.getDialect());
	}
}
