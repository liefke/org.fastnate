package org.fastnate.generator.converter;

import java.util.Date;

import jakarta.persistence.TemporalType;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;

/**
 * Converts a date property of an {@link EntityClass} to SQL.
 *
 * @author Andreas Penski
 */
public class DateConverter extends AbstractDateConverter<Date> {

	/**
	 * Creates a new instance of {@link DateConverter}.
	 *
	 * @param attribute
	 *            the inspected attribute
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map property
	 */
	public DateConverter(final AttributeAccessor attribute, final boolean mapKey) {
		super(attribute, mapKey);
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