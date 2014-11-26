package org.fastnate.generator.converter;

import java.lang.reflect.Field;

import javax.persistence.Column;
import javax.persistence.MapKeyColumn;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;

/**
 * Converts a string property of an {@link EntityClass}.
 * 
 * @author Tobias Liefke
 */
public class StringConverter extends AbstractValueConverter<String> {

	private static final int DEFAULT_COLUMN_LENGTH = 255;

	private final int maxSize;

	private final int minSize;

	private boolean nullable;

	private final Field field;

	/**
	 * Creates a new instance of a StringConverter that accepts strings of arbitrary length.
	 */
	public StringConverter() {
		this.minSize = 0;
		this.maxSize = Integer.MAX_VALUE;
		this.field = null;
	}

	/**
	 * Creates a new instance of a StringConverter for the given field.
	 * 
	 * @param field
	 *            the field to convert
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map field
	 */
	public StringConverter(final Field field, final boolean mapKey) {
		this.field = field;

		// Build constraints
		if (mapKey) {
			final MapKeyColumn column = field.getAnnotation(MapKeyColumn.class);
			this.nullable = column != null && !column.nullable();

			this.maxSize = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
			this.minSize = 0;
		} else {
			final Column column = field.getAnnotation(Column.class);
			this.nullable = column != null && !column.nullable() || field.getAnnotation(NotNull.class) != null;

			final Size size = field.getAnnotation(Size.class);
			if (size != null) {
				this.maxSize = Math.min(column != null ? column.length() : Integer.MAX_VALUE, size.max());
				this.minSize = size.min();
			} else {
				this.maxSize = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
				this.minSize = 0;
			}
		}
	}

	@Override
	public String getExpression(final String value, final GeneratorContext context) {
		// Check constraints
		if (value.length() > this.maxSize) {
			throw new IllegalArgumentException("The length of the given string value (" + value.length()
					+ ") exceeds the maximum allowed length of " + this.field + " (" + this.maxSize + "): " + value);
		}
		if (value.length() < this.minSize) {
			throw new IllegalArgumentException("The length of the given string value (" + value.length()
					+ ") is shorter that the minimum allowed length of " + this.field + " (" + this.minSize + "): "
					+ value);
		}
		if (value.length() == 0 && this.nullable && context.getDialect().isEmptyStringEqualToNull()) {
			throw new IllegalArgumentException("The given string is empty, but field " + this.field
					+ " must be not empty for the current database type.");
		}

		// Replace all special characters
		return context.getDialect().quoteString(value);
	}

}
