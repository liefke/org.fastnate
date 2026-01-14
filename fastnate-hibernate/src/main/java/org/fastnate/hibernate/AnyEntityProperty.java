package org.fastnate.hibernate;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.validation.constraints.NotNull;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.EntityProperty;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;

/**
 * Describes a property of an {@link EntityClass} that references an entity according to a {@link Any} mapping.
 *
 * @param <E>
 *            The type of the container entity
 * @param <T>
 *            The base type of the target entity
 *
 * @author Tobias Liefke
 */
public final class AnyEntityProperty<E, T> extends EntityProperty<E, T> {

	/** Contains information about the discriminator column. */
	private final AnyMapping<T> anyMapping;

	/**
	 * Builds a new {@link AnyEntityProperty} according to the given attribute.
	 *
	 * @param context
	 *            the generator context.
	 * @param containerTable
	 *            the table that contains our columns
	 * @param attribute
	 *            the accessor of the attribute
	 * @param associationOverride
	 *            optional {@link AssociationOverride} configuration
	 * @param attributeOverride
	 *            optional {@link AttributeOverride} configuration
	 */
	public AnyEntityProperty(final GeneratorContext context, final GeneratorTable containerTable,
			final AttributeAccessor attribute, final AssociationOverride associationOverride,
			final AttributeOverride attributeOverride) {
		super(context, containerTable, attribute, associationOverride);
		this.anyMapping = new AnyMapping<>(context, attribute, containerTable, attributeOverride);
	}

	@Override
	protected MappingInformation findEntityMappingInformation(final AttributeAccessor attribute) {
		return new MappingInformation(attribute.getType(),
				!attribute.isAnnotationPresent(NotNull.class) && attribute.getAnnotation(Any.class).optional(), null,
				false);
	}

	@Override
	public String getPredicate(final E entity) {
		final String predicate = super.getPredicate(entity);
		if (predicate == null) {
			return null;
		}
		final T value = getValue(entity);
		if (value == null) {
			return predicate;
		}
		return '(' + predicate + " AND " + this.anyMapping.getPredicate(value) + ')';
	}

	@Override
	protected void writeColumnExpression(final TableStatement statement, final T value,
			final ColumnExpression expression) {
		super.writeColumnExpression(statement, value, expression);
		this.anyMapping.setColumnValue(statement, value);
	}

}
