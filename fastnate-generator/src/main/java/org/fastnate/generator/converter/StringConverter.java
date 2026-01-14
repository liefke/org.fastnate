package org.fastnate.generator.converter;

import jakarta.persistence.Column;
import jakarta.persistence.MapKeyColumn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts a string property of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 */
public class StringConverter implements ValueConverter<String> {

	private static final int DEFAULT_COLUMN_LENGTH = 255;

	private final int maxSize;

	private final int minSize;

	private boolean nullable;

	private final String attributeName;

	/**
	 * Creates a new instance of a StringConverter that accepts strings of arbitrary length.
	 */
	public StringConverter() {
		this.minSize = 0;
		this.maxSize = Integer.MAX_VALUE;
		this.attributeName = "column";
		this.nullable = true;
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
		this.attributeName = attribute.toString();

		// Build constraints
		if (mapKey) {
			final MapKeyColumn column = attribute.getAnnotation(MapKeyColumn.class);
			this.nullable = column == null || column.nullable();

			this.maxSize = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
			this.minSize = 0;
		} else {
			final Column column = attribute.getAnnotation(Column.class);
			final int columnLength = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
			this.nullable = (column == null || column.nullable()) && !attribute.isAnnotationPresent(NotNull.class);

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

	/**
	 * Creates a new instance of a StringConverter for the given column definition.
	 *
	 * @param column
	 *            contains the optional definition, the converter uses the defaults if the column is {@code null}
	 * @param nullable
	 *            indicates that the column may be {@code null} according to other settings (may be overridden by the
	 *            column definition)
	 */
	public StringConverter(final Column column, final boolean nullable) {
		this.attributeName = column == null || column.name().length() == 0 ? "column" : column.name();
		this.minSize = 0;
		this.maxSize = column != null ? column.length() : DEFAULT_COLUMN_LENGTH;
		this.nullable = nullable && (column == null || column.nullable());
	}

	@Override
	public ColumnExpression getExpression(final String value, final GeneratorContext context) {
		// Check constraints
		if (value.length() > this.maxSize) {
			throw new IllegalArgumentException("The length of the given string value (" + value.length()
					+ ") exceeds the maximum allowed length of " + this.attributeName + " (" + this.maxSize + "): "
					+ value);
		}
		if (value.length() < this.minSize) {
			throw new IllegalArgumentException("The length of the given string value (" + value.length()
					+ ") is smaller than the minimum allowed length of " + this.attributeName + " (" + this.minSize
					+ "): " + value);
		}
		if (value.length() == 0 && !this.nullable && context.getDialect().isEmptyStringEqualToNull()) {
			throw new IllegalArgumentException("The given string is empty, but property " + this.attributeName
					+ " must be not empty for the current database type.");
		}

		// Replace all special characters
		return PrimitiveColumnExpression.create(value, context.getDialect());
	}

}
