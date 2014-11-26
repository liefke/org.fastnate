package org.fastnate.generator.converter;

import java.lang.reflect.Field;
import java.util.Calendar;

import javax.persistence.TemporalType;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;

/**
 * Describes a calendar property of an {@link EntityClass}.
 *
 * @author apenski
 */
public class CalendarConverter extends TemporalConverter<Calendar> {

	/**
	 * Creates a new instance of {@link CalendarConverter}.
	 *
	 * @param field
	 *            the inspected field
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map field
	 */
	public CalendarConverter(final Field field, final boolean mapKey) {
		super(field, mapKey);
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
	public String getExpression(final Calendar value, final GeneratorContext context) {
		return getExpression(value.getTime(), context);
	}

}