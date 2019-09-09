package org.fastnate.data.properties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;

/**
 * Base class for all importers that work with {@link PropertyConverter}s.
 *
 * @author Tobias Liefke
 */
public abstract class PropertyDataImporter {

	/** The default list of known converters for each class. */
	private static final Map<Class<?>, PropertyConverter<?>> DEFAULT_CONVERTERS = new HashMap<>();

	static {
		// Initialize the default converters
		DEFAULT_CONVERTERS.put(String.class, (targetClass, value) -> value);
		DEFAULT_CONVERTERS.put(Number.class, new NumberConverter());
		DEFAULT_CONVERTERS.put(Boolean.class, new BooleanConverter());
		DEFAULT_CONVERTERS.put(Enum.class, new EnumConverter());
		DEFAULT_CONVERTERS.put(Character.class, new CharacterConverter());
		DEFAULT_CONVERTERS.put(Date.class, new DateConverter<>());
		DEFAULT_CONVERTERS.put(LocalDate.class, (targetType, value) -> LocalDate.parse(value));
		DEFAULT_CONVERTERS.put(LocalDateTime.class, (targetType, value) -> LocalDateTime.parse(value));
		DEFAULT_CONVERTERS.put(LocalTime.class, (targetType, value) -> LocalTime.parse(value));
		DEFAULT_CONVERTERS.put(byte[].class, (targetType, value) -> Base64.getDecoder().decode(value));
	}

	/** The converters to use as default for specific property classes. */
	private final Map<Class<?>, PropertyConverter<?>> converters = new HashMap<>(DEFAULT_CONVERTERS);

	/**
	 * Registers the converter to use for a specific type.
	 *
	 * Only nessecary, if the default converters are not enough.
	 *
	 * @param type
	 *            the type of the property
	 * @param converter
	 *            used to convert that type from a String in the import file to a Java value
	 */
	public <T> void addConverter(final Class<T> type, final PropertyConverter<T> converter) {
		this.converters.put(type, converter);
	}

	/**
	 * Finds the converter for the given type.
	 *
	 * @param propertyClass
	 *            the type of the primitive property to convert
	 * @return the converter or {@code null} if none is found
	 */
	protected <T> PropertyConverter<T> findConverter(final Class<T> propertyClass) {
		if (propertyClass == null) {
			return null;
		}
		if (propertyClass.isPrimitive()) {
			return findConverter(ClassUtils.primitiveToWrapper(propertyClass));
		}
		PropertyConverter<?> converter = this.converters.get(propertyClass);
		if (converter != null) {
			return (PropertyConverter<T>) converter;
		}
		for (final Class<?> interfaceClass : propertyClass.getInterfaces()) {
			converter = findConverter(interfaceClass);
			if (converter != null) {
				return (PropertyConverter<T>) converter;
			}
		}
		if (propertyClass.getSuperclass() == null) {
			return null;
		}
		return (PropertyConverter<T>) findConverter(propertyClass.getSuperclass());
	}

}
