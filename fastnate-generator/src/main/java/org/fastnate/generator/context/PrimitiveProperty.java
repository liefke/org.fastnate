package org.fastnate.generator.context;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;

import lombok.Getter;

import org.apache.commons.lang.ClassUtils;
import org.fastnate.generator.DefaultValue;
import org.fastnate.generator.converter.BooleanConverter;
import org.fastnate.generator.converter.CalendarConverter;
import org.fastnate.generator.converter.CharConverter;
import org.fastnate.generator.converter.DateConverter;
import org.fastnate.generator.converter.EnumConverter;
import org.fastnate.generator.converter.LobConverter;
import org.fastnate.generator.converter.NumberConverter;
import org.fastnate.generator.converter.SerializableConverter;
import org.fastnate.generator.converter.StringConverter;
import org.fastnate.generator.converter.UnsupportedTypeConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.InsertStatement;

/**
 * Describes a singular primitive property of an {@link EntityClass}.
 *
 * A primitive property is not only a Java primitive, but it includes all properties that are of a type that is easily
 * mapped to an SQL expression using a {@link ValueConverter}.
 *
 * This includes:
 * <ul>
 * <li>Java primitive types (boolean, char, byte, short, int, long, float, double)</li>
 * <li>Their wrapper types ({@link Boolean}, {@link Character}, {@link Byte}, {@link Short}, {@link Integer},
 * {@link Long}, {@link Float}, {@link Double})</li>
 * <li>{@link String}</li>
 * <li>{@link BigInteger} and {@link BigDecimal}</li>
 * <li>{@link Date} and its subclasses ({@link java.sql.Date}, {@link java.sql.Time} and {@link java.sql.Timestamp})</li>
 * <li>{@link Calendar}</li>
 * <li>byte arrays ({@code byte[]} and {@code Byte[]})</li>
 * <li>character array ({@code char[]} and {@code Character[]})</li>
 * <li>Enumerated types ({@link Enum})</li>
 * <li>User-defined serializable types</li>
 * </ul>
 *
 * @param <E>
 *            The type of the container class
 * @param <T>
 *            The type of the primitive
 * @author Tobias Liefke
 * @author Andreas Penski
 */
@Getter
public class PrimitiveProperty<E, T> extends SingularProperty<E, T> {

	/**
	 * Creates a converter for a primitive type.
	 *
	 * @param <T>
	 *            the generic type
	 * @param <E>
	 *            the element type
	 * @param attribute
	 *            the accessor for the attribute that contains the value, not nessecarily of the target type
	 * @param targetType
	 *            the primitive type
	 * @param mapKey
	 *            indicates that the converter is for the key of a map
	 * @return the converter or {@code null} if no converter is available
	 */
	@SuppressWarnings("unchecked")
	public static <T, E extends Enum<E>> ValueConverter<T> createConverter(final AttributeAccessor attribute,
			final Class<T> targetType, final boolean mapKey) {
		if (attribute.hasAnnotation(Lob.class)) {
			return (ValueConverter<T>) new LobConverter();
		}
		final Class<T> type = ClassUtils.primitiveToWrapper(targetType);
		if (String.class == type) {
			return (ValueConverter<T>) new StringConverter(attribute, mapKey);
		} else if (Character.class == type) {
			return (ValueConverter<T>) new CharConverter();
		} else if (Boolean.class == type) {
			return (ValueConverter<T>) new BooleanConverter();
		} else if (Number.class.isAssignableFrom(type)) {
			return (ValueConverter<T>) new NumberConverter();
		} else if (Date.class.isAssignableFrom(type)) {
			return (ValueConverter<T>) new DateConverter(attribute, mapKey);
		} else if (Calendar.class.isAssignableFrom(type)) {
			return (ValueConverter<T>) new CalendarConverter(attribute, mapKey);
		} else if (Enum.class.isAssignableFrom(type)) {
			return (ValueConverter<T>) new EnumConverter<>(attribute, (Class<E>) type, mapKey);
		} else if (Serializable.class.isAssignableFrom(type)) {
			return (ValueConverter<T>) new SerializableConverter();
		} else {
			return (ValueConverter<T>) new UnsupportedTypeConverter(attribute);
		}
	}

	private static boolean isRequired(final AttributeAccessor attribute) {
		final Basic basic = attribute.getAnnotation(Basic.class);
		return basic != null && !basic.optional() || attribute.hasAnnotation(NotNull.class)
				|| attribute.getType().isPrimitive();
	}

	/** The current context. */
	private final GeneratorContext context;

	/** The table of the column. */
	private final String table;

	/** The column name in the table. */
	private final String column;

	/** Indicates if the property required. */
	private final boolean required;

	/** The converter for any value. */
	private final ValueConverter<T> converter;

	/** The default value. */
	private final String defaultValue;

	/**
	 * Instantiates a new primitive property.
	 *
	 * @param context
	 *            the current context
	 * @param table
	 *            the table that the column belongs to
	 * @param attribute
	 *            the attribute of the property
	 * @param columnMetadata
	 *            the column metadata
	 */
	@SuppressWarnings("unchecked")
	public PrimitiveProperty(final GeneratorContext context, final String table, final AttributeAccessor attribute,
			final Column columnMetadata) {
		super(attribute);

		this.context = context;
		this.table = table;

		this.column = columnMetadata == null || columnMetadata.name().length() == 0 ? attribute.getName()
				: columnMetadata.name();
		this.required = columnMetadata != null && !columnMetadata.nullable() || isRequired(attribute);

		this.converter = createConverter(attribute, (Class<T>) attribute.getType(), false);

		final DefaultValue defaultValueAnnotation = attribute.getAnnotation(DefaultValue.class);
		if (defaultValueAnnotation != null) {
			this.defaultValue = defaultValueAnnotation.value();
		} else {
			this.defaultValue = null;
		}
	}

	@Override
	public void addInsertExpression(final E entity, final InsertStatement statement) {
		final T value = getValue(entity);
		if (value != null) {
			statement.addValue(getColumn(), this.converter.getExpression(value, this.context));
		} else if (this.defaultValue != null) {
			statement.addValue(getColumn(), this.converter.getExpression(this.defaultValue, this.context));
		} else {
			failIfRequired();
			if (this.context.isWriteNullValues()) {
				statement.addValue(this.column, "null");
			}
		}
	}

	@Override
	public String getExpression(final E entity, final boolean whereExpression) {
		final T value = getValue(entity);
		if (value == null) {
			if (this.defaultValue != null) {
				return this.converter.getExpression(this.defaultValue, this.context);
			}
			return "null";
		}
		return this.converter.getExpression(value, this.context);
	}

	@Override
	public String getPredicate(final E entity) {
		final T value = getValue(entity);
		if (value == null) {
			return this.column + " IS NULL";
		}

		return this.column + " = " + this.converter.getExpression(value, this.context);
	}

}
