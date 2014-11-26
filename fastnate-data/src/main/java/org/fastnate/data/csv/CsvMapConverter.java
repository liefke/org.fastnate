package org.fastnate.data.csv;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Converts a string from a CSV file to an object by using an explicit mapping.
 *
 * @author Tobias Liefke
 * @param <K>
 *            the type of the key
 * @param <T>
 *            the type of the value
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CsvMapConverter<K, T> implements CsvPropertyConverter<T> {

	/**
	 * Creates a new instance of a CsvMapConverter that uses numbers to lookup the values.
	 *
	 * @param map
	 *            mapping from the number to the values
	 * @param numberClass
	 *            the concrete class of the numbers
	 * @return the created converter
	 */
	public static <N extends Number, V> CsvMapConverter<N, V> create(final Map<N, V> map, final Class<N> numberClass) {
		try {
			return new CsvMapConverter<>(map, numberClass.getConstructor(String.class));
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("The given number class is not instantiable.", e);
		}
	}

	/**
	 * Creates a new instance of a CsvMapConverter that uses strings to lookup the values.
	 *
	 * @param map
	 *            mapping from the string to the value
	 * @return the created converter
	 */
	public static <V> CsvMapConverter<String, V> create(final Map<String, V> map) {
		return new CsvMapConverter<>(map, null);
	}

	private final Map<K, T> map;

	private final Constructor<K> keyConstructor;

	@Override
	public T convert(final Class<? extends T> targetType, final String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		K key;
		if (this.keyConstructor != null) {
			try {
				key = this.keyConstructor.newInstance(value);
			} catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			key = (K) value;
		}
		final T t = this.map.get(key);
		if (t == null) {
			throw new IllegalArgumentException("Can't find value in map: " + value);
		}
		return t;
	}
}