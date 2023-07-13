package org.fastnate.generator.converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.fastnate.generator.RelativeDate;
import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

import lombok.RequiredArgsConstructor;

/**
 * Base class for converting a {@link java.util.Date}, {@link java.util.Calendar}, {@link java.sql.Date},
 * {@link java.sql.Time} or {@link java.sql.Timestamp} property of an {@link EntityClass} to a SQL expression.
 *
 * @author Andreas Penski
 * @author Heiko Schefter
 * @param <T>
 *            the date type
 */
@RequiredArgsConstructor
public abstract class AbstractDateConverter<T> implements ValueConverter<T> {

	private static final Map<Pattern, DateFormat> DEFAULT_FORMATS = new LinkedHashMap<>();

	static {
		// ISO 8601 formats for date
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"), new SimpleDateFormat("yyyy-MM-dd"));

		// ISO 8601 formats for time
		DEFAULT_FORMATS.put(Pattern.compile("\\d{2}:\\d{2}:\\d{2}"), new SimpleDateFormat("HH:mm:ss"));
		DEFAULT_FORMATS.put(Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"), new SimpleDateFormat("HH:mm:ss.S"));

		// ISO 8601 formats for timestamps
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}\\d{2}?"),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sszzz"));
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}"),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
		DEFAULT_FORMATS.put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}\\d{2}?"),
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSzzz"));
	}

	private final TemporalType type;

	/**
	 * Creates a new instance of this {@link AbstractDateConverter}.
	 *
	 * @param attribute
	 *            the inspected attribute
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map property
	 */
	public AbstractDateConverter(final AttributeAccessor attribute, final boolean mapKey) {
		TemporalType temporalType = TemporalType.TIMESTAMP;
		if (mapKey) {
			final MapKeyTemporal temporal = attribute.getAnnotation(MapKeyTemporal.class);
			if (temporal != null) {
				temporalType = temporal.value();
			}
		} else {
			final Temporal temporal = attribute.getAnnotation(Temporal.class);
			if (temporal != null) {
				temporalType = temporal.value();
			}
		}
		this.type = temporalType;
	}

	/**
	 * Converts the given value into an SQL expression.
	 *
	 * @param value
	 *            the value to convert
	 * @param context
	 *            the current context
	 * @return the SQL expression
	 */
	public ColumnExpression getExpression(final Date value, final GeneratorContext context) {
		return new PrimitiveColumnExpression<>(value, context.getDialect().convertToDatabaseDate(value, this.type),
				v -> context.getDialect().convertTemporalValue(value, this.type));
	}

	/** Use database independent default values. */
	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		if ("CURRENT_TIMESTAMP".equals(defaultValue)) {
			return getExpression(RelativeDate.NOW, context);
		} else if ("CURRENT_DATE".equals(defaultValue)) {
			return getExpression(RelativeDate.TODAY, context);
		}
		for (final Map.Entry<Pattern, DateFormat> format : DEFAULT_FORMATS.entrySet()) {
			if (format.getKey().matcher(defaultValue).matches()) {
				try {
					return getExpression(format.getValue().parse(defaultValue), context);
				} catch (final ParseException e) {
					throw new IllegalArgumentException("Can't parse " + defaultValue + " as date", e);
				}
			}
		}
		return new PrimitiveColumnExpression<>(defaultValue, Function.identity());
	}

}