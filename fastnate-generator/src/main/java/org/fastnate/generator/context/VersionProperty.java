package org.fastnate.generator.context;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import org.fastnate.generator.RelativeDate;

/**
 * Represents a property marked with {@link Version}.
 *
 * @param <E>
 *            The type of the container class
 * @param <T>
 *            The type of the property
 *
 * @author Tobias Liefke
 */
public class VersionProperty<E, T> extends PrimitiveProperty<E, T> {

	/**
	 * Creates a new instance of {@link VersionProperty}.
	 *
	 * @param context
	 *            the current context
	 * @param table
	 *            the table that the column belongs to
	 * @param attribute
	 *            the accessor of the version attribute
	 * @param column
	 *            the column annotation
	 */
	public VersionProperty(final GeneratorContext context, final GeneratorTable table,
			final AttributeAccessor attribute, final Column column) {
		super(context, table, attribute, column);
	}

	@Override
	protected String getDefaultValue(final AttributeAccessor attribute) {
		final String defaultValue = super.getDefaultValue(attribute);
		if (defaultValue == null) {
			final Class<?> type = attribute.getType();
			if (Number.class.isAssignableFrom(type)) {
				return "0";
			}
			if (Date.class.isAssignableFrom(type)) {
				final Temporal temporal = attribute.getAnnotation(Temporal.class);
				final TemporalType temporalType = temporal != null ? temporal.value() : TemporalType.TIMESTAMP;
				return getContext().getDialect().convertTemporalValue(RelativeDate.NOW, temporalType);
			}
		}
		return defaultValue;
	}

}
