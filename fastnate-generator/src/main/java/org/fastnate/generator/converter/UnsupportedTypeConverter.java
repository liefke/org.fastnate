package org.fastnate.generator.converter;

import java.lang.reflect.Field;

import org.fastnate.generator.context.GeneratorContext;

import lombok.RequiredArgsConstructor;

/**
 * Converter used for all types that we can't convert.
 * 
 * Used to generate a lazy exception - only if someone fills a field of unsupported type, we need to throw an exception.
 * 
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class UnsupportedTypeConverter extends AbstractValueConverter<Object> {

	private final Field field;

	@Override
	public String getExpression(final Object value, final GeneratorContext context) {
		throw new IllegalArgumentException("Unsupported type for field: " + this.field);
	}

}
