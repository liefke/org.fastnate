package org.fastnate.generator.converter;

import jakarta.persistence.Lob;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Generates the expression for a {@link Lob property}.
 *
 * @author Tobias Liefke
 */
public class LobConverter implements ValueConverter<Object> {

	@Override
	public ColumnExpression getExpression(final Object value, final GeneratorContext context) {
		if (value instanceof String) {
			return PrimitiveColumnExpression.create((String) value, context.getDialect());
		}
		if (value instanceof char[]) {
			return PrimitiveColumnExpression.create(new String((char[]) value), context.getDialect());
		}
		if (value instanceof byte[]) {
			return new PrimitiveColumnExpression<>((byte[]) value, context.getDialect()::createBlobExpression);
		}
		throw new IllegalArgumentException("Can't handle LOB of type " + value.getClass());
	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression((Object) defaultValue, context);
	}

}
