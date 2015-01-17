package org.fastnate.generator.converter;

import lombok.Getter;

import org.fastnate.generator.context.EmbeddedProperty;
import org.fastnate.generator.context.GeneratorContext;

/**
 * Converts the reference to an entity to an expression that uses the sequence value of that entity.
 *
 * @author Tobias Liefke
 */
@Getter
public class EntityConverter extends AbstractValueConverter<Object> {

	/**
	 * Creates an expression for an entity.
	 *
	 * @param entity
	 *            the entity
	 * @param mappedId
	 *            the property that contains the id, only interesting if the id is an {@link EmbeddedProperty}
	 * @param context
	 *            the current database context
	 * @param whereExpression
	 *            indicates that the reference is used in a "where" statement
	 * @return the expression using the sequence of that entity or {@code null} if the entity was not written up to now
	 */
	public static String getEntityReference(final Object entity, final String mappedId, final GeneratorContext context,
			final boolean whereExpression) {
		return context.getDescription(entity).getEntityReference(entity, mappedId, whereExpression);
	}

	@Override
	public String getExpression(final Object value, final GeneratorContext context) {
		return getEntityReference(value, null, context, true);
	}
}
