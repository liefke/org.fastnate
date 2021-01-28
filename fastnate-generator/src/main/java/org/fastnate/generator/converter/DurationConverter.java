package org.fastnate.generator.converter;

import java.time.Duration;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts a {@link Duration} to an SQL expression.
 *
 * @author Tobias Liefke
 */
public class DurationConverter implements ValueConverter<Duration> {

	@Override
	public ColumnExpression getExpression(final Duration value, final GeneratorContext context) {
		return PrimitiveColumnExpression.create(value.toNanos(), context.getDialect());
	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression(Duration.parse(defaultValue), context);
	}

}
