package org.fastnate.data.properties;

import java.util.function.Function;

/**
 * Used to convert a string from an import file to a Java object.
 *
 * @param <T>
 *            the (minimum) type of the target value
 */
public interface PropertyConverter<T> {

	/**
	 * Converts a function to a PropertyConverter.
	 *
	 * @param converter
	 *            the converter that needs no information of the target type
	 * @return the given converter wrapped as {@code PropertyConverter}
	 */
	static <T> PropertyConverter<T> of(final Function<String, T> converter) {
		return (targetType, value) -> converter.apply(value);
	}

	/**
	 * Converts a value from import file to Java.
	 *
	 * @param targetType
	 *            the type of the target property
	 * @param value
	 *            the value in the import file
	 * @return the value in Java
	 */
	T convert(Class<? extends T> targetType, String value);
}