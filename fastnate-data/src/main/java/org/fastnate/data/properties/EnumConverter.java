package org.fastnate.data.properties;

import org.apache.commons.lang3.StringUtils;

/**
 * Converts a string in an import file to an {@link Enum} value.
 *
 * @author Tobias Liefke
 */
public class EnumConverter implements PropertyConverter<Enum<?>> {

	@Override
	@SuppressWarnings("rawtypes")
	public Enum<?> convert(final Class<? extends Enum<?>> targetType, final String value) {
		return StringUtils.isBlank(value) ? null : Enum.valueOf((Class) targetType, value.trim());
	}
}