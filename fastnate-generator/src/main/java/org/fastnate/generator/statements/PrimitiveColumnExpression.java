package org.fastnate.generator.statements;

import java.util.function.Function;

import org.fastnate.generator.dialect.GeneratorDialect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A {@link ColumnExpression} which contains a primitive value.
 *
 * @author Tobias Liefke
 * @param <T>
 *            the type of the primitive value
 */
@Getter
@RequiredArgsConstructor
public class PrimitiveColumnExpression<T> implements ColumnExpression {

	/** Represents an expression of a {@code null} value. */
	public static final PrimitiveColumnExpression<Object> NULL = new PrimitiveColumnExpression<>(null, t -> "null");

	/**
	 * Creates a new instance of {@link PrimitiveColumnExpression} for a number.
	 *
	 * @param value
	 *            the number value
	 * @param dialect
	 *            the dialect of the current database
	 * @return the new expression
	 */
	public static final <N extends Number> PrimitiveColumnExpression<N> create(final N value,
			final GeneratorDialect dialect) {
		return new PrimitiveColumnExpression<>(value, dialect::convertNumberValue);
	}

	/**
	 * Creates a new instance of a {@link PrimitiveColumnExpression} for a string.
	 *
	 * @param value
	 *            the string value
	 * @param dialect
	 *            the dialect of the current database
	 * @return the new expression
	 */
	public static final PrimitiveColumnExpression<String> create(final String value, final GeneratorDialect dialect) {
		return new PrimitiveColumnExpression<>(value, dialect::quoteString);
	}

	/** The primitive value (which may be used in an prepared statement). */
	private final T value;

	/** Converts the primitive value to a SQL string. */
	private final Function<T, String> converter;

	@Override
	public String toSql() {
		return this.converter.apply(this.value);
	}

	@Override
	public String toString() {
		return toSql();
	}

}
