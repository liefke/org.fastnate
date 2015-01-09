package org.fastnate.generator.converter;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;

/**
 * Describes a char property of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 */
public class CharConverter extends AbstractValueConverter<Character> {

	@Override
	public String getExpression(final Character value, final GeneratorContext context) {
		return context.getDialect().quoteString(String.valueOf(value));
	}

	@Override
	public String getExpression(final String defaultValue, final GeneratorContext context) {
		return defaultValue.length() <= 1 ? getExpression(defaultValue.charAt(0), context) : defaultValue;
	}

}
