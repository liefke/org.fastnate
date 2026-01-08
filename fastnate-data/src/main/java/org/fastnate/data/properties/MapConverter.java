package org.fastnate.data.properties;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

/**
 * Converts a string from an import file to an object by using an explicit mapping.
 *
 * @author Tobias Liefke
 * @param <K>
 *            the type of the key
 * @param <T>
 *            the type of the value
 */
@RequiredArgsConstructor
public class MapConverter<K, T> implements PropertyConverter<T>, Function<String, T> {

	/**
	 * Creates a new instance of a MapConverter that uses numbers to lookup the values.
	 *
	 * @param map
	 *            mapping from the number to the values
	 * @param numberClass
	 *            the concrete class of the numbers
	 * @return the created converter
	 */
	public static <N extends Number, V> MapConverter<N, V> create(final Map<N, V> map, final Class<N> numberClass) {
		try {
			final Constructor<N> keyConstructor = numberClass.getConstructor(String.class);

			return new MapConverter<>(map, key -> {
				try {
					return keyConstructor.newInstance(key);
				} catch (final ReflectiveOperationException e) {
					throw new IllegalArgumentException(e);
				}
			});
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("The given number class is not instantiable.", e);
		}
	}

	/**
	 * Creates a new instance of a MapConverter that uses strings to lookup the values.
	 *
	 * @param map
	 *            mapping from the string to the value
	 * @return the created converter
	 */
	public static <V> MapConverter<String, V> create(final Map<String, V> map) {
		return new MapConverter<>(map, Function.identity());
	}

	/** The mapping from the keys in the import file to the target value. */
	private final Map<K, T> map;

	/** Converts the string in the import file to the key in the mapping. */
	private final Function<String, K> keyConverter;

	@Override
	public T apply(final String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		final K key = this.keyConverter.apply(value);
		final T result = this.map.get(key);
		if (result == null) {
			throw new IllegalArgumentException("Can't find value in map: " + value);
		}
		return result;
	}

	@Override
	public T convert(final Class<? extends T> targetType, final String value) {
		return apply(value);
	}

}