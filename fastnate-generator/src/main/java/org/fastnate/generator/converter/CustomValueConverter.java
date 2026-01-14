package org.fastnate.generator.converter;

import jakarta.persistence.AttributeConverter;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

import lombok.RequiredArgsConstructor;

/**
 * A value converter that uses an {@link AttributeConverter}.
 *
 * @author Tobias Liefke
 * @param <T>
 *            the type of the attribute
 * @param <C>
 *            the type of the database column
 */
@RequiredArgsConstructor
public class CustomValueConverter<T, C> implements ValueConverter<T> {

	private final AttributeConverter<T, C> customConverter;

	private final ValueConverter<C> valueConverter;

	@Override
	public ColumnExpression getExpression(final T value, final GeneratorContext context) {
		final C databaseValue = this.customConverter.convertToDatabaseColumn(value);
		if (databaseValue == null) {
			return PrimitiveColumnExpression.NULL;
		}
		return this.valueConverter.getExpression(databaseValue, context);
	}

}
