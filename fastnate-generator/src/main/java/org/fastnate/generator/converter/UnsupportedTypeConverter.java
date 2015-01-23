package org.fastnate.generator.converter;

import lombok.RequiredArgsConstructor;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.GeneratorContext;

/**
 * Converter used for all types that we can't convert.
 *
 * Used to generate a lazy exception - only if someone fills a property of unsupported type, we need to throw an
 * exception.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class UnsupportedTypeConverter extends AbstractValueConverter<Object> {

	private final AttributeAccessor attribute;

	@Override
	public String getExpression(final Object value, final GeneratorContext context) {
		throw new IllegalArgumentException("Unsupported type for property: " + this.attribute);
	}

}
