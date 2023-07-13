package org.fastnate.generator.converter;

import java.util.Calendar;

import jakarta.persistence.TemporalType;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;

/**
 * Converts a calendar property of an {@link EntityClass} to SQL.
 *
 * @author Andreas Penski
 */
public class CalendarConverter extends AbstractDateConverter<Calendar> {

	/**
	 * Creates a new instance of {@link CalendarConverter}.
	 *
	 * @param attribute
	 *            the inspected attribute
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map property
	 */
	public CalendarConverter(final AttributeAccessor attribute, final boolean mapKey) {
		super(attribute, mapKey);
	}

	/**
	 * Creates a new instance of {@link CalendarConverter}.
	 *
	 * @param type
	 *            the temporal type of the property
	 */
	public CalendarConverter(final TemporalType type) {
		super(type);
	}

	@Override
	public ColumnExpression getExpression(final Calendar value, final GeneratorContext context) {
		return getExpression(value.getTime(), context);
	}

}