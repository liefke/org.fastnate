package org.fastnate.generator.converter;

import javax.persistence.Lob;

import org.fastnate.generator.context.GeneratorContext;

/**
 * Generates the expression for a {@link Lob property}.
 * 
 * @author Tobias Liefke
 */
public class LobConverter implements ValueConverter<Object> {

	@Override
	public String getExpression(final Object value, final GeneratorContext context) {
		if (value instanceof String) {
			return context.getDialect().quoteString((String) value);
		}
		if (value instanceof char[]) {
			return context.getDialect().quoteString(new String((char[]) value));
		}
		if (value instanceof byte[]) {
			return context.getDialect().createBlobExpression((byte[]) value);
		}
		throw new IllegalArgumentException("Can't handle LOB of type " + value.getClass());
	}

	@Override
	public String getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression((Object) defaultValue, context);
	}

}
