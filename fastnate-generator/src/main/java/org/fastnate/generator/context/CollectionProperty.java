package org.fastnate.generator.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.persistence.AssociationOverride;
import javax.persistence.ElementCollection;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.statements.EntityStatement;

import lombok.Getter;

/**
 * Describes a property of an {@link EntityClass} that contains more than one value.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container entity
 * @param <T>
 *            The type of the entity inside of the collection
 */
@Getter
public class CollectionProperty<E, T> extends PluralProperty<E, Collection<T>, T> {

	/**
	 * Indicates that the given attribute references a collection and may be used by an {@link CollectionProperty}.
	 *
	 * @param attribute
	 *            the attribute to check
	 * @return {@code true} if an {@link CollectionProperty} may be created for the given attribute
	 */
	static boolean isCollectionProperty(final AttributeAccessor attribute) {
		return Collection.class.isAssignableFrom(attribute.getType())
				&& (attribute.isAnnotationPresent(OneToMany.class) || attribute.isAnnotationPresent(ManyToMany.class)
						|| attribute.isAnnotationPresent(ElementCollection.class));
	}

	/** The name of the column that saves the order of the entries in the collection. */
	private final String orderColumn;

	/**
	 * Creates a new collection property.
	 *
	 * @param sourceClass
	 *            the description of the current inspected class that contains this property
	 * @param attribute
	 *            accessor to the represented attribute
	 * @param override
	 *            the configured assocation override
	 */
	public CollectionProperty(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride override) {
		super(sourceClass, attribute, override, 0);

		// Read a potentially defined order column
		final OrderColumn orderColumnDef = attribute.getAnnotation(OrderColumn.class);
		this.orderColumn = orderColumnDef == null ? null
				: orderColumnDef.name().length() == 0 ? attribute.getName() + "_ORDER" : orderColumnDef.name();
	}

	@Override
	public List<EntityStatement> createPostInsertStatements(final E entity) {
		if (getMappedBy() != null && this.orderColumn == null) {
			return Collections.emptyList();
		}

		final List<EntityStatement> result = new ArrayList<>();
		final String sourceId = EntityConverter.getEntityReference(entity, getMappedId(), getContext(), false);
		int index = 0;
		final Collection<T> collection = getValue(entity);
		// Check for uniqueness, if no order column is given
		if (this.orderColumn == null && collection instanceof List
				&& new HashSet<>(collection).size() < collection.size()) {
			throw new IllegalArgumentException(
					"At least one duplicate value in " + this + " of " + entity + ": " + collection);
		}
		for (final T value : collection) {
			final EntityStatement statement = createValueStatement(entity, sourceId, String.valueOf(index++), value);
			if (statement != null) {
				result.add(statement);
			}
		}

		return result;
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		if (isEmbedded()) {
			final List<Object> result = new ArrayList<>();
			for (final T value : getValue(entity)) {
				for (final Property<T, ?> property : getEmbeddedProperties()) {
					result.addAll(property.findReferencedEntities(value));
				}
			}
			return result;
		} else if (isEntityReference()) {
			return getValue(entity);
		}
		return Collections.emptySet();
	}

	@Override
	protected String getKeyColumn() {
		return this.orderColumn;
	}

	@Override
	public Collection<T> getValue(final E entity) {
		final Collection<T> value = super.getValue(entity);
		return value == null ? Collections.<T> emptySet() : value;
	}

}
