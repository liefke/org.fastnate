package org.fastnate.generator.converter;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;

import lombok.RequiredArgsConstructor;

/**
 * Base class of description of a temporal property of an {@link EntityClass}.
 *
 * @author Andreas Penski
 * @author Heiko Schefter
 * @param <T>
 *            the temporal type (e.g. Date or {@link Calendar})
 */
@RequiredArgsConstructor
public abstract class TemporalConverter<T> extends AbstractValueConverter<T> {

	private final TemporalType type;

	/**
	 * Creates a new instance of this {@link TemporalConverter}.
	 *
	 * @param field
	 *            the inspected field
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map field
	 */
	public TemporalConverter(final Field field, final boolean mapKey) {
		TemporalType temporalType = TemporalType.TIMESTAMP;
		if (mapKey) {
			final MapKeyTemporal temporal = field.getAnnotation(MapKeyTemporal.class);
			if (temporal != null) {
				temporalType = temporal.value();
			}
		} else {
			final Temporal temporal = field.getAnnotation(Temporal.class);
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
	public String getExpression(final Date value, final GeneratorContext context) {
		return context.getDialect().convertTemporalValue(value, this.type);
	}

}