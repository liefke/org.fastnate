package org.fastnate.generator.converter;

import java.util.Date;

import javax.persistence.TemporalType;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.PropertyAccessor;

/**
 * Converts a date property of an {@link EntityClass} to SQL.
 *
 * @author Andreas Penski
 */
public class DateConverter extends TemporalConverter<Date> {

	/**
	 * Creates a new instance of {@link DateConverter}.
	 *
	 * @param property
	 *            the inspected property
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map property
	 */
	public DateConverter(final PropertyAccessor property, final boolean mapKey) {
		super(property, mapKey);
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