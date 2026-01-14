package org.fastnate.hibernate;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.ElementCollection;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.MapProperty;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.ManyToAny;

/**
 * Describes a property of an {@link EntityClass} that references a map of entities according to a {@link ManyToAny}
 * mapping.
 *
 * @param <E>
 *            The type of the container entity
 * @param <K>
 *            The type of the key of the map
 * @param <T>
 *            The type of the entity inside of the collection
 *
 * @author Tobias Liefke
 */
public class ManyToAnyMapProperty<E, K, T> extends MapProperty<E, K, T> {

	/** Contains information about the discriminator column. */
	private final AnyMapping<T> anyMapping;

	/**
	 * Creates a new collection property that is mapped with {@link ManyToAny}.
	 *
	 * @param sourceClass
	 *            the description of the current inspected class that contains this property
	 * @param attribute
	 *            accessor to the represented attribute
	 * @param associationOverride
	 *            the configured assocation override
	 * @param attributeOverride
	 *            the configured attribute override, if we reference an {@link ElementCollection}
	 */
	public ManyToAnyMapProperty(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride associationOverride, final AttributeOverride attributeOverride) {
		super(sourceClass, attribute, associationOverride, attributeOverride);
		this.anyMapping = new AnyMapping<>(getContext(), attribute, getTable(), attributeOverride);
	}

	@Override
	protected EntityMappingInformation findEntityMappingInformation(final AttributeAccessor attribute,
			final AssociationOverride associationOverride, final int valueClassParamIndex) {
		final ManyToAny manyToAny = attribute.getAnnotation(ManyToAny.class);
		if (manyToAny != null) {
			return new EntityMappingInformation(getPropertyArgument(attribute, void.class, valueClassParamIndex), null,
					false, false);
		}
		return super.findEntityMappingInformation(attribute, associationOverride, valueClassParamIndex);
	}

	@Override
	protected void writeValueExpression(final TableStatement statement, final T value,
			final ColumnExpression expression) {
		super.writeValueExpression(statement, value, expression);
		this.anyMapping.setColumnValue(statement, value);
	}

}
