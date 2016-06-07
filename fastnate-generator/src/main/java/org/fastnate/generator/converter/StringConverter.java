package org.fastnate.generator.converter;

import javax.persistence.Column;
import javax.persistence.MapKeyColumn;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.fastnate.generator.context.AttributeAccessor;
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

	private final AttributeAccessor attribute;

	/**
	 * Creates a new instance of a StringConverter that accepts strings of arbitrary length.
	 */
	public StringConverter() {
		this.minSize = 0;
		this.maxSize = Integer.MAX_VALUE;
		this.attribute = null;
	}

	/**
	 * Creates a new instance of a StringConverter for the given attribute.
	 *
	 * @param attribute
	 *            the attribute to convert
	 * @param mapKey
	 *            indicates that the converter is used for the key of a map property
	 */
	public StringConverter(final AttributeAccessor attribute, final boolean mapKey) {
		this.attribute = attribute;

		// Build constraints
		if (mapKey) {
			final MapKeyColumn column = attribute.getAnnotation(MapKeyColumn.class);
			this.nullable = column != null && !column.nullable();

			this.maxSize = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
			this.minSize = 0;
		} else {
			final Column column = attribute.getAnnotation(Column.class);
			final int columnLength = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
			this.nullable = column != null && !column.nullable() || attribute.hasAnnotation(NotNull.class);

			final Size size = attribute.getAnnotation(Size.class);
			if (size != null) {
				this.maxSize = size.max() < Integer.MAX_VALUE ? size.max() : columnLength;
				this.minSize = size.min();
			} else {
				this.maxSize = columnLength;
				this.minSize = 0;
			}
		}
	}

	@Override
	public String getExpression(final String value, final GeneratorContext context) {
		// Check constraints
		if (value.length() > this.maxSize) {
			throw new IllegalArgumentException("The length of the given string value (" + value.length()
					+ ") exceeds the maximum allowed length of " + this.attribute + " (" + this.maxSize + "): "
					+ value);
		}
		if (value.length() < this.minSize) {
			throw new IllegalArgumentException("The length of the given string value (" + value.length()
					+ ") is shorter that the minimum allowed length of " + this.attribute + " (" + this.minSize + "): "
					+ value);
		}
		if (value.length() == 0 && this.nullable && context.getDialect().isEmptyStringEqualToNull()) {
			throw new IllegalArgumentException("The given string is empty, but property " + this.attribute
					+ " must be not empty for the current database type.");
		}

		// Replace all special characters
		return context.getDialect().quoteString(value);
	}

}
