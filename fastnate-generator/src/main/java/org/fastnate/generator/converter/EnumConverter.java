package org.fastnate.generator.converter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MapKeyEnumerated;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Describes a enum property of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            the type of the enum
 */
public class EnumConverter<E extends Enum<E>> implements ValueConverter<E> {

	private final Class<E> targetType;

	private final EnumType exportType;

	/**
	 * Creates a new instance of this {@link EnumConverter}.
	 *
	 * @param attribute
	 *            the inspected attribute
	 * @param targetType
	 *            the type of the enum
	 * @param mapKey
	 *            indicates that the converter is for the key of a map
	 */
	public EnumConverter(final AttributeAccessor attribute, final Class<E> targetType, final boolean mapKey) {
		this.targetType = targetType;
		EnumType enumType = EnumType.ORDINAL;
		if (mapKey) {
			final MapKeyEnumerated enumerated = attribute.getAnnotation(MapKeyEnumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
		} else {
			final Enumerated enumerated = attribute.getAnnotation(Enumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
		}
		this.exportType = enumType;
	}

	@Override
	public ColumnExpression getExpression(final E value, final GeneratorContext context) {
		switch (this.exportType) {
			case STRING:
				return PrimitiveColumnExpression.create(value.name(), context.getDialect());
			case ORDINAL:
			default:
				return PrimitiveColumnExpression.create(value.ordinal(), context.getDialect());
		}
	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression(Enum.valueOf(this.targetType, defaultValue), context);
	}

}
