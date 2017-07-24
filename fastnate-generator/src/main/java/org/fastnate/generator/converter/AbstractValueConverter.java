package org.fastnate.generator.converter;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Base class for {@link ValueConverter}.
 *
 * @author Tobias Liefke
 * @param <T>
 *            The type of the handled values
 */
public abstract class AbstractValueConverter<T> implements ValueConverter<T> {

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		return PrimitiveColumnExpression.create(defaultValue, context.getDialect());
	}

}
