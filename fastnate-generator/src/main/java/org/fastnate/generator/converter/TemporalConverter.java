package org.fastnate.generator.converter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.function.Function;

import org.fastnate.generator.DefaultValue;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts the {@link Temporal} types from {@link java.time} to SQL expressions.
 *
 * @author Tobias Liefke
 * @param <T>
 *            the type of the temporal
 */
public class TemporalConverter<T extends Temporal> implements ValueConverter<T> {

	/** The method that creates the actual value from a string, which was defined as {@link DefaultValue}. */
	private Method parse;

	/** The method that creates the SQL string from a temporal. */
	private Function<T, String> toString;

	/**
	 * Creates a new converter.
	 *
	 * @param type
	 *            the type of the attribute
	 */
	public TemporalConverter(final Class<?> type) {
		try {
			final Method method = type.getMethod("parse", CharSequence.class);
			if (Modifier.isAbstract(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())
					|| method.getReturnType() != type) {
				throw new ModelException(type + " is not supported by the TemporalConverter!");
			}
			this.parse = method;
			if (type == ZonedDateTime.class) {
				this.toString = (Function<T, String>) (Function<ZonedDateTime, String>) t -> t.toLocalDateTime()
						.toString() + t.getOffset().toString();
			} else {
				this.toString = Temporal::toString;
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ModelException(type + " is not supported by the TemporalConverter!", e);
		}

	}

	@Override
	public ColumnExpression getExpression(final String defaultValue, final GeneratorContext context) {
		try {
			return getExpression((T) this.parse.invoke(null, defaultValue), context);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException("Could not convert " + defaultValue + " to a temporal value", e);
		}
	}

	@Override
	public ColumnExpression getExpression(final T value, final GeneratorContext context) {
		final GeneratorDialect dialect = context.getDialect();
		return new PrimitiveColumnExpression<>(value, t -> dialect.quoteString(this.toString.apply(t)));
	}

}
