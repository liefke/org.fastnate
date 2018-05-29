package org.fastnate.generator.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;

/**
 * Converts a {@link Serializable} to a SQL expression.
 *
 * @author Tobias Liefke
 */
public class SerializableConverter implements ValueConverter<Serializable> {

	/** Serializable objects tend to be big, so start not to small with the buffer. */
	private static final int DEFAULT_BUFFER_SIZE = 512;

	@Override
	public ColumnExpression getExpression(final Serializable value, final GeneratorContext context) {
		try {
			// Serialize object
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
			try (ObjectOutputStream stream = new ObjectOutputStream(buffer)) {
				stream.writeObject(value);
			}
			return new PrimitiveColumnExpression<>(buffer.toByteArray(), context.getDialect()::createBlobExpression);
		} catch (final IOException e) {
			// Should only happen, if the object was not correctly serialized
			throw new IllegalStateException(e);
		}
	}

}
