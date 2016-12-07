package org.fastnate.data.csv;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Converts a string from a CSV file to a number.
 *
 * @author Tobias Liefke
 */
public class CsvNumberConverter implements CsvPropertyConverter<Number> {

	@Override
	public Number convert(final Class<? extends Number> targetType, final String value) {
		if (StringUtils.isBlank(value)) {
			if (targetType.isPrimitive()) {
				return convert(targetType, "0");
			}
			return null;
		}
		if (targetType == Number.class) {
			return new Float(value);
		}
		final Class<? extends Number> wrapperType = targetType.isPrimitive() ? ClassUtils.primitiveToWrapper(targetType)
				: targetType;
		try {
			return wrapperType.getConstructor(String.class).newInstance(value);
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}
}