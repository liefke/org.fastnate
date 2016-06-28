package org.fastnate.generator.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.persistence.AssociationOverride;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;
import org.fastnate.generator.statements.UpdateStatement;

import com.google.common.base.Preconditions;

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

	private static String buildOrderColumn(final AttributeAccessor attribute) {
		final OrderColumn orderColumnDef = attribute.getAnnotation(OrderColumn.class);
		return orderColumnDef == null ? null
				: orderColumnDef.name().length() == 0 ? attribute.getName() + "_ORDER" : orderColumnDef.name();
	}

	/**
	 * Indicates that the given attribute references a collection and may be used by an {@link CollectionProperty}.
	 *
	 * @param attribute
	 *            the attribute to check
	 * @return {@code true} if an {@link CollectionProperty} may be created for the given attribute
	 */
	static boolean isCollectionProperty(final AttributeAccessor attribute) {
		return (attribute.isAnnotationPresent(OneToMany.class) || attribute.isAnnotationPresent(ManyToMany.class)
				|| attribute.isAnnotationPresent(ElementCollection.class))
				&& Collection.class.isAssignableFrom(attribute.getType());
	}

	private static boolean useTargetTable(final AttributeAccessor attribute, final AssociationOverride override) {
		final JoinColumn joinColumn = override != null && override.joinColumns().length > 0 ? override.joinColumns()[0]
				: attribute.getAnnotation(JoinColumn.class);
		final JoinTable joinTable = override != null && override.joinTable() != null ? override.joinTable()
				: attribute.getAnnotation(JoinTable.class);
		return joinColumn != null && joinTable == null;

	}

	/** Indicates that this property is defined by another property on the target type. */
	private final String mappedBy;

	/** The target class. */
	private final Class<T> targetClass;

	/** The description of the target class, {@code null} if not an entity. */
	private final EntityClass<T> targetEntityClass;

	/** The converter for the target value, {@code null} if not a primitive value. */
	private final ValueConverter<T> targetConverter;

	/** The name of the modified table. */
	private final String table;

	/** The name of the column that contains the id of the entity. */
	private String idColumn;

	/** The name of the column that contains the value (or the id of the value). */
	private final String valueColumn;

	/** The name of the column that saves the order of the entries in the collection. */
	private final String orderColumn;

	/** Indicates to use a column of the target table. */
	private final boolean useTargetTable;

	/**
	 * Creates a new collection property.
	 *
	 * @param sourceClass
	 *            the description of the current inspected class of the accessor
	 * @param attribute
	 *            accessor to the represented attribute
	 * @param override
	 *            the configured assocation override
	 */
	@SuppressWarnings("unchecked")
	public CollectionProperty(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride override) {
		super(sourceClass.getContext(), attribute);

		// Read a potentially defined order column
		this.orderColumn = buildOrderColumn(attribute);

		// Check if we are OneToMany or ManyToMany or ElementCollection and initialize accordingly
		final CollectionTable collectionTable = attribute.getAnnotation(CollectionTable.class);
		final ElementCollection values = attribute.getAnnotation(ElementCollection.class);
		if (values != null) {
			// We are the owning side of the mapping
			this.mappedBy = null;
			this.useTargetTable = false;

			// Initialize the table and id column name
			this.table = buildTableName(collectionTable, sourceClass.getEntityName() + '_' + attribute.getName());
			this.idColumn = buildIdColumn(attribute, override, collectionTable,
					sourceClass.getEntityName() + '_' + sourceClass.getIdColumn(attribute));

			// Initialize the target description and columns
			this.targetClass = getPropertyArgument(attribute, values.targetClass(), 0);
			if (this.targetClass.isAnnotationPresent(Embeddable.class)) {
				buildEmbeddedProperties(this.targetClass);
				this.targetEntityClass = null;
				this.targetConverter = null;
				this.valueColumn = null;
			} else {
				this.targetEntityClass = sourceClass.getContext().getDescription(this.targetClass);
				// Check for primitive value
				this.targetConverter = this.targetEntityClass == null
						? PrimitiveProperty.createConverter(attribute, this.targetClass, false) : null;
				this.valueColumn = buildValueColumn(attribute, attribute.getName());
			}
		} else {
			// Entity mapping, either OneToMany or ManyToMany

			final OneToMany oneToMany = attribute.getAnnotation(OneToMany.class);
			if (oneToMany == null) {
				final ManyToMany manyToMany = attribute.getAnnotation(ManyToMany.class);
				Preconditions.checkArgument(manyToMany != null,
						attribute + " is neither declared as OneToMany nor ManyToMany nor ElementCollection");
				this.targetClass = getPropertyArgument(attribute, manyToMany.targetEntity(), 0);
				this.mappedBy = manyToMany.mappedBy().length() == 0 ? null : manyToMany.mappedBy();
				this.useTargetTable = this.mappedBy != null;
			} else {
				this.targetClass = getPropertyArgument(attribute, oneToMany.targetEntity(), 0);
				this.mappedBy = oneToMany.mappedBy().length() == 0 ? null : oneToMany.mappedBy();
				this.useTargetTable = this.mappedBy != null || useTargetTable(attribute, override);
			}

			// Resolve the target entity class
			this.targetEntityClass = sourceClass.getContext().getDescription(this.targetClass);

			// An entity mapping needs an entity class
			Preconditions.checkArgument(this.targetEntityClass != null,
					"Collection accessor " + attribute + " needs an entity type");

			// No primitive value
			this.targetConverter = null;

			// Initialize the table and column names
			if (this.mappedBy != null) {
				// Bidirectional - use the columns of the target class
				this.table = this.targetEntityClass.getTable();
				// Find the mappedBy property later - may be that is not created in the target class up to now
				final Property<? super T, ?> idProperty = this.targetEntityClass.getIdProperty();
				Preconditions.checkArgument(idProperty instanceof SingularProperty,
						"Can only handle singular properties for ID in mapped by " + attribute);
				this.valueColumn = buildValueColumn(attribute, this.targetEntityClass.getIdColumn(attribute));
			} else if (this.useTargetTable) {
				// Unidirectional and join column is in the table of the target class
				this.table = this.targetEntityClass.getTable();
				this.idColumn = buildIdColumn(attribute, override, null, null,
						attribute.getName() + '_' + sourceClass.getIdColumn(attribute));
				this.valueColumn = buildValueColumn(attribute, this.targetEntityClass.getIdColumn(attribute));
			} else {
				// Unidirectional and we need a mapping table
				final JoinTable joinTable = attribute.getAnnotation(JoinTable.class);
				this.table = buildTableName(attribute, override, joinTable, collectionTable,
						sourceClass.getTable() + '_' + this.targetEntityClass.getTable());
				this.idColumn = buildIdColumn(attribute, override, joinTable, collectionTable,
						sourceClass.getEntityName() + '_' + sourceClass.getIdColumn(attribute));
				this.valueColumn = buildValueColumn(attribute,
						attribute.getName() + '_' + this.targetEntityClass.getIdColumn(attribute));
			}
		}
	}

	@Override
	public List<EntityStatement> createPostInsertStatements(final E entity) {
		if (this.mappedBy != null && this.orderColumn == null) {
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
			if (isEmbedded()) {
				result.add(createEmbeddedPropertiesStatement(sourceId, index++, value));
			} else {
				final EntityStatement statement = createDirectPropertyStatement(entity, sourceId, index++, value);
				if (statement != null) {
					result.add(statement);
				}
			}
		}

		return result;
	}

	private EntityStatement createDirectPropertyStatement(final E entity, final String sourceId, final int index,
			final T value) {
		final String target;
		if (value == null) {
			target = "null";
		} else {
			if (this.targetConverter != null) {
				target = this.targetConverter.getExpression(value, getContext());
			} else {
				target = this.targetEntityClass.getEntityReference(value, getMappedId(), this.useTargetTable);
				if (target == null) {
					// Not created up to now
					this.targetEntityClass.markPendingUpdates(value, entity, this, index);
					return null;
				}
			}
		}

		if (this.idColumn == null && this.mappedBy != null) {
			final Property<T, ?> mappedByProperty = this.targetEntityClass.getProperties().get(this.mappedBy);
			Preconditions.checkArgument(mappedByProperty != null,
					"Could not find property: " + this.mappedBy + " in " + this.targetClass);
			Preconditions.checkArgument(mappedByProperty instanceof SingularProperty,
					"Can only handle singular properties for mapped by in " + getAttribute().getElement());
			this.idColumn = ((SingularProperty<?, ?>) mappedByProperty).getColumn();
		}

		final EntityStatement stmt;
		if (this.useTargetTable) {
			// Unidirectional, but from target table
			if (value == null) {
				return null;
			}
			stmt = new UpdateStatement(this.table, this.valueColumn, target);
			if (this.mappedBy == null) {
				stmt.addValue(this.idColumn, sourceId);
			}
		} else {
			stmt = new InsertStatement(this.table);
			stmt.addValue(this.idColumn, sourceId);
			stmt.addValue(this.valueColumn, target);
		}
		if (this.orderColumn != null) {
			stmt.addValue(this.orderColumn, String.valueOf(index));
		}
		return stmt;
	}

	private InsertStatement createEmbeddedPropertiesStatement(final String sourceId, final int index, final T value) {
		final InsertStatement stmt = new InsertStatement(this.table);

		stmt.addValue(this.idColumn, sourceId);
		if (this.orderColumn != null) {
			stmt.addValue(this.orderColumn, String.valueOf(index));
		}

		for (final SingularProperty<T, ?> property : getEmbeddedProperties()) {
			property.addInsertExpression(value, stmt);
		}
		return stmt;
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		if (this.targetEntityClass != null) {
			return getValue(entity);
		} else if (isEmbedded()) {
			final List<Object> result = new ArrayList<>();
			for (final T value : getValue(entity)) {
				for (final Property<T, ?> property : getEmbeddedProperties()) {
					result.addAll(property.findReferencedEntities(value));
				}
			}
			return result;
		}
		return Collections.emptySet();
	}

	@Override
	public List<EntityStatement> generatePendingStatements(final E entity, final Object writtenEntity,
			final Object... arguments) {
		final String sourceId = EntityConverter.getEntityReference(entity, getMappedId(), getContext(), false);
		final EntityStatement statement = createDirectPropertyStatement(entity, sourceId,
				((Integer) arguments[0]).intValue(), (T) writtenEntity);
		return statement == null ? Collections.<EntityStatement> emptyList() : Collections.singletonList(statement);
	}

	@Override
	public Collection<T> getValue(final E entity) {
		final Collection<T> value = super.getValue(entity);
		return value == null ? Collections.<T> emptySet() : value;
	}

}
