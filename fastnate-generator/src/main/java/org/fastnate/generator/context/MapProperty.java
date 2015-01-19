package org.fastnate.generator.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.AssociationOverride;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;

import lombok.Getter;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

import com.google.common.base.Preconditions;

/**
 * Describes a property of an {@link EntityClass} that is a {@link Map}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container entity
 * @param <K>
 *            The type of the key of the map
 * @param <T>
 *            The type of the entity inside of the collection
 */
@Getter
public class MapProperty<E, K, T> extends PluralProperty<E, Map<K, T>, T> {

	private static String buildKeyColumn(final MapKeyColumn keyColumn, final String defaultKeyColumn) {
		if (keyColumn != null && keyColumn.name().length() > 0) {
			return keyColumn.name();
		}
		return defaultKeyColumn;
	}

	private static String buildKeyColumn(final MapKeyJoinColumn keyColumn, final String defaultKeyColumn) {
		if (keyColumn != null && keyColumn.name().length() > 0) {
			return keyColumn.name();
		}
		return defaultKeyColumn;
	}

	/**
	 * Indicates that the given property references a map and may be used by an {@link MapProperty}.
	 *
	 * @param property
	 *            the property to check
	 * @return {@code true} if an {@link MapProperty} may be created for the given field
	 */
	static boolean isMapProperty(final PropertyAccessor property) {
		return (property.hasAnnotation(OneToMany.class) || property.hasAnnotation(ManyToMany.class) || property
				.hasAnnotation(ElementCollection.class)) && Map.class.isAssignableFrom(property.getType());
	}

	/** Indicates that this property is defined by another property on the target type. */
	private final String mappedBy;

	/** The class of the key of the map. */
	private final Class<K> keyClass;

	/** The description of the key class, {@code null} if not an entity. */
	private final EntityClass<K> keyEntityClass;

	/** The converter for the target value, {@code null} if not a primitive value. */
	private final ValueConverter<K> keyConverter;

	/** The class of the value of the map. */
	private final Class<T> valueClass;

	/** The description of the value class, {@code null} if not an entity. */
	private final EntityClass<T> valueEntityClass;

	/** The converter for the target value, {@code null} if not a primitive value. */
	private final ValueConverter<T> valueConverter;

	/** The name of the modified table. */
	private final String table;

	/** The name of the column that contains the id of the entity. */
	private final String idColumn;

	/** The name of the column that contains the key. */
	private final String keyColumn;

	/** The name of the column that contains the value (or the id of the value). */
	private final String valueColumn;

	/**
	 * Creates a new map property.
	 *
	 * @param sourceClass
	 *            the description of the current inspected class of the field
	 * @param accessor
	 *            the represented property
	 * @param override
	 *            the configured assocation override
	 */
	@SuppressWarnings("unchecked")
	public MapProperty(final EntityClass<?> sourceClass, final PropertyAccessor accessor,
			final AssociationOverride override) {
		super(sourceClass.getContext(), accessor);

		// Initialize the key description
		final MapKeyClass keyClassAnnotation = accessor.getAnnotation(MapKeyClass.class);
		this.keyClass = getPropertyArgument(accessor, keyClassAnnotation != null ? keyClassAnnotation.value()
				: void.class, 0);
		this.keyEntityClass = sourceClass.getContext().getDescription(this.keyClass);
		if (this.keyEntityClass != null) {
			// Entity key
			this.keyConverter = null;
			this.keyColumn = buildKeyColumn(accessor.getAnnotation(MapKeyJoinColumn.class), accessor.getName() + "_KEY");
		} else {
			// Primitive key
			this.keyConverter = PrimitiveProperty.createConverter(accessor, this.keyClass, true);
			this.keyColumn = buildKeyColumn(accessor.getAnnotation(MapKeyColumn.class), accessor.getName() + "_KEY");
		}

		// Initialize the value description

		// Check if we are OneToMany or ManyToMany or ElementCollection and initialize accordingly
		final ElementCollection values = accessor.getAnnotation(ElementCollection.class);
		if (values != null) {
			// We are the owning side of the mapping
			this.mappedBy = null;

			// Initialize the table and id column name
			final CollectionTable collectionTable = accessor.getAnnotation(CollectionTable.class);
			this.table = buildTableName(collectionTable, sourceClass.getEntityName() + '_' + accessor.getName());
			this.idColumn = buildIdColumn(accessor, override, collectionTable, sourceClass.getEntityName() + '_'
					+ sourceClass.getIdColumn(accessor));

			// Initialize the target class description and columns
			this.valueClass = getPropertyArgument(accessor, values.targetClass(), 1);
			if (this.valueClass.isAnnotationPresent(Embeddable.class)) {
				buildEmbeddedProperties(this.valueClass);
				this.valueEntityClass = null;
				this.valueConverter = null;
				this.valueColumn = null;
			} else {
				this.valueEntityClass = sourceClass.getContext().getDescription(this.valueClass);
				// Check for primitive value
				this.valueConverter = this.valueEntityClass == null ? PrimitiveProperty.createConverter(accessor,
						this.valueClass, false) : null;
				this.valueColumn = buildValueColumn(accessor, accessor.getName());
			}
		} else {
			// Entity mapping, either OneToMany or ManyToMany

			final OneToMany oneToMany = accessor.getAnnotation(OneToMany.class);
			if (oneToMany == null) {
				final ManyToMany manyToMany = accessor.getAnnotation(ManyToMany.class);
				Preconditions.checkArgument(manyToMany != null, accessor
						+ " is neither declared as OneToMany nor ManyToMany nor ElementCollection");
				this.valueClass = getPropertyArgument(accessor, manyToMany.targetEntity(), 1);
				this.mappedBy = manyToMany.mappedBy().length() == 0 ? null : manyToMany.mappedBy();
			} else {
				this.valueClass = getPropertyArgument(accessor, oneToMany.targetEntity(), 1);
				this.mappedBy = oneToMany.mappedBy().length() == 0 ? null : oneToMany.mappedBy();
			}

			// Resolve the target entity class
			this.valueEntityClass = sourceClass.getContext().getDescription(this.valueClass);

			// An entity mapping needs an entity class
			Preconditions.checkArgument(this.valueClass != null, "Map field " + accessor + " needs an entity as value");

			// No primitive value
			this.valueConverter = null;

			// Initialize the table and column names
			if (this.mappedBy != null) {
				// Bidirectional - use the column of the target class
				this.table = null;
				this.idColumn = null;
				this.valueColumn = null;
			} else {
				// Unidirectional and we need a mapping table
				final JoinTable joinTable = accessor.getAnnotation(JoinTable.class);
				final CollectionTable collectionTable = accessor.getAnnotation(CollectionTable.class);
				this.table = buildTableName(accessor, override, joinTable, collectionTable, sourceClass.getTable()
						+ '_' + this.valueEntityClass.getTable());
				this.idColumn = buildIdColumn(accessor, override, joinTable, collectionTable, sourceClass.getTable()
						+ '_' + sourceClass.getIdColumn(accessor));
				this.valueColumn = buildValueColumn(accessor,
						accessor.getName() + '_' + this.valueEntityClass.getIdColumn(accessor));
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
		for (final Map.Entry<K, T> entry : getValue(entity).entrySet()) {
			String key;
			if (entry.getKey() == null) {
				key = "null";
			} else if (this.keyEntityClass != null) {
				key = EntityConverter.getEntityReference(entry.getKey(), getMappedId(), getContext(), false);
			} else {
				key = this.keyConverter.getExpression(entry.getKey(), getContext());
			}

			if (isEmbedded()) {
				result.add(createEmbeddedPropertiesStatement(sourceId, key, entry.getValue()));
			} else {
				final EntityStatement statement = createDirectPropertyStatement(entity, sourceId, key, entry.getValue());
				if (statement != null) {
					result.add(statement);
				}
			}
		}

		return result;
	}

	private EntityStatement createDirectPropertyStatement(final E entity, final String sourceId, final String key,
			final T value) {
		String target;
		if (value == null) {
			target = "null";
		} else {
			if (this.valueConverter != null) {
				target = this.valueConverter.getExpression(value, getContext());
			} else {
				target = this.valueEntityClass.getEntityReference(value, getMappedId(), false);
				if (target == null) {
					// Not created up to now
					this.valueEntityClass.markPendingUpdates(value, entity, this, key);
					return null;
				}
			}
		}

		final InsertStatement stmt = new InsertStatement(this.table, getContext().getDialect());
		stmt.addValue(this.idColumn, sourceId);
		stmt.addValue(this.keyColumn, key);
		stmt.addValue(this.valueColumn, target);
		return stmt;
	}

	private InsertStatement createEmbeddedPropertiesStatement(final String sourceId, final String key, final T value) {
		final InsertStatement stmt = new InsertStatement(this.table, getContext().getDialect());

		stmt.addValue(this.idColumn, sourceId);
		stmt.addValue(this.keyColumn, key);

		for (final SingularProperty<T, ?> property : getEmbeddedProperties()) {
			property.addInsertExpression(value, stmt);
		}
		return stmt;
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		Collection<?> keyEntities = Collections.emptyList();
		Collection<?> valueEntities = Collections.emptyList();
		if (this.keyEntityClass != null) {
			keyEntities = getValue(entity).keySet();
		}
		if (this.valueEntityClass != null) {
			valueEntities = getValue(entity).values();
		} else if (isEmbedded()) {
			final ArrayList<Object> entities = new ArrayList<>();
			for (final T value : getValue(entity).values()) {
				for (final Property<T, ?> property : getEmbeddedProperties()) {
					entities.addAll(property.findReferencedEntities(value));
				}
			}
			valueEntities = entities;
		}
		if (keyEntities.isEmpty()) {
			return valueEntities;
		} else if (valueEntities.isEmpty()) {
			return keyEntities;
		}
		final ArrayList<Object> entities = new ArrayList<>(keyEntities.size() + valueEntities.size());
		entities.addAll(keyEntities);
		entities.addAll(valueEntities);
		return entities;
	}

	@Override
	public List<EntityStatement> generatePendingStatements(final E entity, final Object writtenEntity,
			final Object... arguments) {
		final String sourceId = EntityConverter.getEntityReference(entity, getMappedId(), getContext(), false);
		final EntityStatement statement = createDirectPropertyStatement(entity, (String) arguments[0], sourceId,
				(T) writtenEntity);
		return statement == null ? Collections.<EntityStatement> emptyList() : Collections.singletonList(statement);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<K, T> getValue(final E entity) {
		final Map<K, T> value = super.getValue(entity);
		return value == null ? Collections.EMPTY_MAP : value;
	}

}
