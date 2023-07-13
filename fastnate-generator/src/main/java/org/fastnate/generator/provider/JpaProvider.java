package org.fastnate.generator.provider;

import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.TemporalType;

import org.apache.commons.lang3.ClassUtils;
import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.PrimitiveProperty;
import org.fastnate.generator.converter.BooleanConverter;
import org.fastnate.generator.converter.CalendarConverter;
import org.fastnate.generator.converter.CharConverter;
import org.fastnate.generator.converter.CustomValueConverter;
import org.fastnate.generator.converter.DateConverter;
import org.fastnate.generator.converter.DurationConverter;
import org.fastnate.generator.converter.EnumConverter;
import org.fastnate.generator.converter.LobConverter;
import org.fastnate.generator.converter.NumberConverter;
import org.fastnate.generator.converter.SerializableConverter;
import org.fastnate.generator.converter.StringConverter;
import org.fastnate.generator.converter.TemporalConverter;
import org.fastnate.generator.converter.UnsupportedTypeConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.util.ClassUtil;

/**
 * Encapsulates details specific to the current JPA implementation.
 *
 * @author Tobias Liefke
 */
public interface JpaProvider {

	/**
	 * Creates a converter for a primitive database type.
	 *
	 * @param <T>
	 *            the generic type
	 * @param attributeName
	 *            the name of the attribute that contains the value - only to print that name in case of any error
	 * @param targetType
	 *            the primitive database type
	 * @return the converter or an instance of {@link UnsupportedTypeConverter} if no converter is available
	 */
	@SuppressWarnings("checkstyle:CyclomaticComplexity")
	default <T> ValueConverter<T> createBasicConverter(final String attributeName, final Class<T> targetType) {
		if (String.class == targetType) {
			return (ValueConverter<T>) new StringConverter();
		}
		if (byte[].class == targetType || char[].class == targetType) {
			return (ValueConverter<T>) new LobConverter();
		}
		if (Date.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new DateConverter(TemporalType.TIMESTAMP);
		}
		if (Calendar.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new CalendarConverter(TemporalType.TIMESTAMP);
		}
		if (Temporal.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new TemporalConverter<>(targetType);
		}
		if (Duration.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new DurationConverter();
		}
		final Class<T> type = (Class<T>) ClassUtils.primitiveToWrapper(targetType);
		if (Character.class == type) {
			return (ValueConverter<T>) new CharConverter();
		}
		if (Boolean.class == type) {
			return (ValueConverter<T>) new BooleanConverter();
		}
		if (Number.class.isAssignableFrom(type)) {
			return (ValueConverter<T>) new NumberConverter((Class<Number>) type);
		}
		return createFallbackConverter(attributeName, type);
	}

	/**
	 * Creates a converter for an attribute that has a {@link PrimitiveProperty}.
	 *
	 * @param <T>
	 *            the generic binding of the type of the value
	 * @param attribute
	 *            the accessor for the attribute that contains the value, not nessecarily of the target type
	 * @param targetType
	 *            the type of the value
	 * @param mapKey
	 *            indicates that the converter is for the key of a map
	 * @return the converter or an instance of {@link UnsupportedTypeConverter} if no converter is available
	 */
	default <T> ValueConverter<T> createConverter(final AttributeAccessor attribute, final Class<T> targetType,
			final boolean mapKey) {
		final Convert convert = attribute.getAnnotation(Convert.class);
		if (convert != null && !convert.disableConversion()) {
			final Class<AttributeConverter<T, Object>> converterClass = convert.converter();
			final Class<Object> databaseType = ClassUtil.getActualTypeBinding(converterClass, AttributeConverter.class,
					1);
			try {
				return new CustomValueConverter<>(converterClass.newInstance(),
						createBasicConverter(attribute.getName(), databaseType));
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException("Could not create AttributeConverter: " + converterClass, e);
			}
		}
		if (attribute.isAnnotationPresent(Lob.class)) {
			return (ValueConverter<T>) new LobConverter();
		}
		if (Date.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new DateConverter(attribute, mapKey);
		}
		if (Calendar.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new CalendarConverter(attribute, mapKey);
		}
		if (Enum.class.isAssignableFrom(targetType)) {
			@SuppressWarnings("rawtypes")
			final Class<Enum> enumClass = (Class<Enum>) targetType;
			return (ValueConverter<T>) new EnumConverter<>(attribute, enumClass, mapKey);
		}
		return createBasicConverter(attribute.getName(), targetType);
	}

	/**
	 * Creates the converter for a type that is unknown to us.
	 *
	 * Should be overridden for any provider specific types.
	 *
	 * @param <T>
	 *            the generic type
	 * @param attributeName
	 *            the name of the attribute that contains the value - only to print that name in case of any error
	 * @param targetType
	 *            the primitive database type
	 * @return the converter or an instance of {@link UnsupportedTypeConverter} if no converter is available
	 */
	default <T> ValueConverter<T> createFallbackConverter(final String attributeName, final Class<T> targetType) {
		if (Serializable.class.isAssignableFrom(targetType)) {
			return (ValueConverter<T>) new SerializableConverter();
		}
		return (ValueConverter<T>) new UnsupportedTypeConverter(attributeName);
	}

	/**
	 * The name of the default generator {@link TableGenerator#table()}, if none was specified for a table generator.
	 *
	 * @return the default generator table
	 */
	String getDefaultGeneratorTable();

	/**
	 * The name of the default generator {@link TableGenerator#pkColumnName()}, if none was specified for a table
	 * generator.
	 *
	 * @return the default primary column name for the generator table
	 */
	String getDefaultGeneratorTablePkColumnName();

	/**
	 * The name of the default generator {@link TableGenerator#pkColumnValue()}, if none was specified for a table
	 * generator.
	 *
	 * @return the default primary column value for the generator table
	 */
	String getDefaultGeneratorTablePkColumnValue();

	/**
	 * The name of the default generator {@link TableGenerator#valueColumnName()}, if none was specified for a table
	 * generator.
	 *
	 * @return the default value column name for the generator table
	 */
	String getDefaultGeneratorTableValueColumnName();

	/**
	 * The name of the default {@link SequenceGenerator#sequenceName() sequence}, if none was specified for a sequence
	 * generator.
	 *
	 * @return the default sequence name
	 */
	String getDefaultSequence();

	/**
	 * Initializes this provider from the given settings.
	 *
	 * May as well change the settings according to some other settings found.
	 *
	 * @param settings
	 *            the settings of the generator context
	 */
	void initialize(Properties settings);

	/**
	 * Indicates if the current JPA provider needs always a discriminator column for a JOINED table.
	 *
	 * @return {@code true} to always write a discriminator column, {@code false} if it should be written only if
	 *         explicitly given
	 */
	boolean isJoinedDiscriminatorNeeded();

}
