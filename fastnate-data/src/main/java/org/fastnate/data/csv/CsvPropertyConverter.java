package org.fastnate.data.csv;

/**
 * Used to convert a CSV string to an Java object.
 *
 * @param <T>
 *            the (minimum) type of the target value
 */
public interface CsvPropertyConverter<T> {

	/**
	 * Converts a value from CSV to Java.
	 *
	 * @param targetType
	 *            the type of the target property
	 * @param value
	 *            the value in CSV
	 * @return the value in Java
	 */
	T convert(final Class<? extends T> targetType, final String value);
}