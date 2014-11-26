package org.fastnate.generator.converter;

import java.lang.reflect.Field;
import java.util.Date;

import javax.persistence.TemporalType;

import org.fastnate.generator.context.EntityClass;

/**
 * Describes a date property of an {@link EntityClass}.
 *
 * @author apenski
 */
public class DateConverter extends TemporalConverter<Date> {

	/**
	 * Creates a new instance of {@link DateConverter}.
	 *
	 * @param field
	 *            the inspected field
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map field
	 */
	public DateConverter(final Field field, final boolean mapKey) {
		super(field, mapKey);
	}

	/**
	 * Creates a new instance of {@link DateConverter}.
	 * 
	 * @param type
	 *            the temporal type of the property
	 */
	public DateConverter(final TemporalType type) {
		super(type);
	}

}