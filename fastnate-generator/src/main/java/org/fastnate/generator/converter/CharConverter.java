package org.fastnate.generator.converter;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Describes a char property of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 */
public class CharConverter extends AbstractValueConverter<Character> {

	@Override
	public ColumnExpression getExpression(final Character value, final GeneratorContext context) {
		return PrimitiveColumnExpression.create(String.valueOf(value), context.getDialect());
	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression(defaultValue.length() >= 1 ? defaultValue.charAt(0) : ' ', context);
	}

}
