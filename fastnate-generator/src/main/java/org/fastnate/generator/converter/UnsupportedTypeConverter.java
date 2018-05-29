package org.fastnate.generator.converter;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;

import lombok.RequiredArgsConstructor;

/**
 * Converter used for all types that we can't convert.
 *
 * Used to generate a lazy exception - only if someone fills a property of unsupported type, we need to throw an
 * exception.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class UnsupportedTypeConverter implements ValueConverter<Object> {

	private final AttributeAccessor attribute;

	@Override
	public ColumnExpression getExpression(final Object value, final GeneratorContext context) {
		throw new IllegalArgumentException("Unsupported type for property: " + this.attribute);
	}

}
