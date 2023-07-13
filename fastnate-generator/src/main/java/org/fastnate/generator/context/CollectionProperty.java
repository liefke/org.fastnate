package org.fastnate.generator.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderColumn;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;

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
		return Collection.class.isAssignableFrom(attribute.getType()) && hasPluralAnnotation(attribute);
	}

	/** The name of the column that saves the order of the entries in the collection. */
	private final GeneratorColumn orderColumn;

	/**
	 * Creates a new collection property.
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
	public CollectionProperty(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride associationOverride, final AttributeOverride attributeOverride) {
		super(sourceClass, attribute, associationOverride, attributeOverride, 0);

		// Read a potentially defined order column
		final OrderColumn orderColumnDef = attribute.getAnnotation(OrderColumn.class);
		this.orderColumn = orderColumnDef == null ? null
				: getTable().resolveColumn(
						orderColumnDef.name().length() == 0 ? attribute.getName() + "_ORDER" : orderColumnDef.name());
	}

	@Override
	public void createPostInsertStatements(final StatementsWriter writer, final E entity) throws IOException {
		if (getMappedBy() == null || this.orderColumn != null) {
			final ColumnExpression sourceId = EntityConverter.getEntityReference(entity, getMappedId(), getContext(),
					false);
			int index = 0;
			final GeneratorDialect dialect = getDialect();
			final Collection<T> collection = getValue(entity);
			for (final T value : collection) {
				createValueStatement(writer, entity, sourceId, PrimitiveColumnExpression.create(index++, dialect),
						value);
			}
		}
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
	protected GeneratorColumn getKeyColumn() {
		return this.orderColumn;
	}

	@Override
	public Collection<T> getValue(final E entity) {
		final Collection<T> value = super.getValue(entity);
		return value == null ? Collections.<T> emptySet() : value;
	}

}
