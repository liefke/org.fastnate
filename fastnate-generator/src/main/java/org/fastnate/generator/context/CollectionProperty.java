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

import lombok.Getter;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;
import org.fastnate.generator.statements.UpdateStatement;

import com.google.common.base.Preconditions;

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

	private static String buildOrderColumn(final PropertyAccessor accessor) {
		final OrderColumn orderColumnDef = accessor.getAnnotation(OrderColumn.class);
		return orderColumnDef == null ? null : orderColumnDef.name().length() == 0 ? accessor.getName() + "_order"
				: orderColumnDef.name();
	}

	/**
	 * Indicates that the given accessor references a collection and may be used by an {@link CollectionProperty}.
	 *
	 * @param accessor
	 *            the accessor to check
	 * @return {@code true} if an {@link CollectionProperty} may be created for the given accessor
	 */
	static boolean isCollectionProperty(final PropertyAccessor accessor) {
		return (accessor.hasAnnotation(OneToMany.class) || accessor.hasAnnotation(ManyToMany.class) || accessor
				.hasAnnotation(ElementCollection.class)) && Collection.class.isAssignableFrom(accessor.getType());
	}

	private static boolean useTargetTable(final PropertyAccessor accessor, final AssociationOverride override) {
		final JoinColumn joinColumn = override != null && override.joinColumns().length > 0 ? override.joinColumns()[0]
				: accessor.getAnnotation(JoinColumn.class);
		final JoinTable joinTable = override != null && override.joinTable() != null ? override.joinTable() : accessor
				.getAnnotation(JoinTable.class);
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
	private final String idColumn;

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
	 * @param accessor
	 *            accessor to the represented property
	 * @param override
	 *            the configured assocation override
	 */
	@SuppressWarnings("unchecked")
	public CollectionProperty(final EntityClass<?> sourceClass, final PropertyAccessor accessor,
			final AssociationOverride override) {
		super(sourceClass.getContext(), accessor);

		// Read a potentially defined order column
		this.orderColumn = buildOrderColumn(accessor);

		// Check if we are OneToMany or ManyToMany or ElementCollection and initialize accordingly
		final ElementCollection values = accessor.getAnnotation(ElementCollection.class);
		if (values != null) {
			// We are the owning side of the mapping
			this.mappedBy = null;
			this.useTargetTable = false;

			// Initialize the table and id column name
			final CollectionTable collectionTable = accessor.getAnnotation(CollectionTable.class);
			this.table = buildTableName(collectionTable, sourceClass.getEntityName() + '_' + accessor.getName());
			this.idColumn = buildIdColumn(accessor, override, collectionTable, sourceClass.getEntityName() + '_'
					+ sourceClass.getIdColumn(accessor));

			// Initialize the target description and columns
			this.targetClass = getPropertyArgument(accessor, values.targetClass(), 0);
			if (this.targetClass.isAnnotationPresent(Embeddable.class)) {
				buildEmbeddedProperties(this.targetClass);
				this.targetEntityClass = null;
				this.targetConverter = null;
				this.valueColumn = null;
			} else {
				this.targetEntityClass = sourceClass.getContext().getDescription(this.targetClass);
				// Check for primitive value
				this.targetConverter = this.targetEntityClass == null ? PrimitiveProperty.createConverter(accessor,
						this.targetClass, false) : null;
				this.valueColumn = buildValueColumn(accessor, accessor.getName());
			}
		} else {
			// Entity mapping, either OneToMany or ManyToMany

			final OneToMany oneToMany = accessor.getAnnotation(OneToMany.class);
			if (oneToMany == null) {
				final ManyToMany manyToMany = accessor.getAnnotation(ManyToMany.class);
				Preconditions.checkArgument(manyToMany != null, accessor
						+ " is neither declared as OneToMany nor ManyToMany nor ElementCollection");
				this.targetClass = getPropertyArgument(accessor, manyToMany.targetEntity(), 0);
				this.mappedBy = manyToMany.mappedBy().length() == 0 ? null : manyToMany.mappedBy();
				this.useTargetTable = false;
			} else {
				this.targetClass = getPropertyArgument(accessor, oneToMany.targetEntity(), 0);
				this.mappedBy = oneToMany.mappedBy().length() == 0 ? null : oneToMany.mappedBy();
				this.useTargetTable = useTargetTable(accessor, override);
			}

			// Resolve the target entity class
			this.targetEntityClass = sourceClass.getContext().getDescription(this.targetClass);

			// An entity mapping needs an entity class
			Preconditions.checkArgument(this.targetEntityClass != null, "Collection accessor " + accessor
					+ " needs an entity type");

			// No primitive value
			this.targetConverter = null;

			// Initialize the table and column names
			if (this.mappedBy != null) {
				// Bidirectional - use the column of the target class
				this.table = null;
				this.idColumn = null;
				this.valueColumn = null;
			} else if (this.useTargetTable) {
				// Unidirectional and join column is in the table of the target class
				this.table = this.targetEntityClass.getTable();
				this.idColumn = buildIdColumn(accessor, override, (JoinTable) null, accessor.getName() + '_'
						+ sourceClass.getIdColumn(accessor));
				this.valueColumn = buildValueColumn(accessor, this.targetEntityClass.getIdColumn(accessor));
			} else {
				// Unidirectional and we need a mapping table
				final JoinTable joinTable = accessor.getAnnotation(JoinTable.class);
				this.table = buildTableName(accessor, override, joinTable, sourceClass.getTable() + '_'
						+ this.targetEntityClass.getTable());
				this.idColumn = buildIdColumn(accessor, override, joinTable,
						sourceClass.getTable() + '_' + sourceClass.getIdColumn(accessor));
				this.valueColumn = buildValueColumn(accessor,
						accessor.getName() + '_' + this.targetEntityClass.getIdColumn(accessor));
			}
		}
	}

	@Override
	public List<EntityStatement> buildAdditionalStatements(final E entity) {
		if (this.mappedBy != null) {
			return Collections.emptyList();
		}

		final List<EntityStatement> result = new ArrayList<>();
		final String sourceId = EntityConverter.getEntityReference(entity, getMappedId(), getContext(), false);
		int index = 0;
		final Collection<T> collection = getValue(entity);
		// Check for uniqueness, if no order column is given
		if (this.orderColumn == null && collection instanceof List
				&& new HashSet<>(collection).size() < collection.size()) {
			throw new IllegalArgumentException("At least one duplicate value in " + this + " of " + entity + ": "
					+ collection);
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
		String target;
		if (value == null) {
			target = "null";
		} else {
			if (this.targetConverter != null) {
				target = this.targetConverter.getExpression(value, getContext());
			} else {
				target = this.targetEntityClass.getEntityReference(value, getMappedId(), false);
				if (target == null) {
					// Not created up to now
					this.targetEntityClass.markPendingUpdates(value, entity, this, index);
					return null;
				}
			}
		}

		if (this.useTargetTable) {
			// Unidirectional, but from target table
			if (value == null) {
				return null;
			}
			final UpdateStatement stmt = new UpdateStatement(this.table, this.valueColumn, target);
			stmt.addValue(this.idColumn, sourceId);
			return stmt;
		}
		final InsertStatement stmt = new InsertStatement(this.table, getContext().getDialect());
		stmt.addValue(this.idColumn, sourceId);
		stmt.addValue(this.valueColumn, target);
		if (this.orderColumn != null) {
			stmt.addValue(this.orderColumn, String.valueOf(index));
		}
		return stmt;
	}

	private InsertStatement createEmbeddedPropertiesStatement(final String sourceId, final int index, final T value) {
		final InsertStatement stmt = new InsertStatement(this.table, getContext().getDialect());

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
