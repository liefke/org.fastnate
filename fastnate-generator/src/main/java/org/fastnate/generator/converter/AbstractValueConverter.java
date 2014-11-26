package org.fastnate.generator.converter;

import org.fastnate.generator.context.GeneratorContext;

/**
 * Base class for {@link ValueConverter}.
 * 
 * @author Tobias Liefke
 * @param <T>
 *            The type of the handled values
 */
public abstract class AbstractValueConverter<T> implements ValueConverter<T> {

	@Override
	public String getExpression(final String defaultValue, final GeneratorContext context) {
		return defaultValue;
	}

}
