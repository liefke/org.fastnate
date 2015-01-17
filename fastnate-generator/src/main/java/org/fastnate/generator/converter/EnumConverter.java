package org.fastnate.generator.converter;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MapKeyEnumerated;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.PropertyAccessor;

/**
 * Describes a enum property of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            the type of the enum
 */
public class EnumConverter<E extends Enum<E>> extends AbstractValueConverter<E> {

	private final Class<E> targetType;

	private final EnumType exportType;

	/**
	 * Creates a new instance of this {@link EnumConverter}.
	 *
	 * @param property
	 *            the inspected property
	 * @param targetType
	 *            the type of the enum
	 * @param mapKey
	 *            indicates that the converter is for the key of a map
	 */
	public EnumConverter(final PropertyAccessor property, final Class<E> targetType, final boolean mapKey) {
		this.targetType = targetType;
		EnumType enumType = EnumType.ORDINAL;
		if (mapKey) {
			final MapKeyEnumerated enumerated = property.getAnnotation(MapKeyEnumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
		} else {
			final Enumerated enumerated = property.getAnnotation(Enumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
		}
		this.exportType = enumType;
	}

	@Override
	public String getExpression(final E value, final GeneratorContext context) {
		switch (this.exportType) {
		case STRING:
			return context.getDialect().quoteString(value.name());
		case ORDINAL:
		default:
			return String.valueOf(value.ordinal());
		}
	}

	@Override
	public String getExpression(final String defaultValue, final GeneratorContext context) {
		return getExpression(Enum.valueOf(this.targetType, defaultValue), context);
	}

}
