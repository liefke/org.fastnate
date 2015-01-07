package org.fastnate.generator.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.fastnate.generator.context.GenerationState.PendingState;
import org.fastnate.generator.statements.EntityStatement;
import org.hibernate.annotations.Formula;

/**
 * Describes the DB relevant metadata of an {@link Entity entity class}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The described class
 */
@Getter
public class EntityClass<E> {

	/**
	 * Helper object to use as key in the state map, if we have a {@link GeneratedIdProperty}.
	 *
	 * As the hashcode of entities usually changes if the id changes, we can't use the entity as key itself.
	 *
	 * As result, the hash map is used as linked list.
	 */
	@RequiredArgsConstructor
	private static final class EntityId {

		private final Object entity;

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof EntityId && ((EntityId) obj).entity == this.entity;
		}

		@Override
		public int hashCode() {
			return 0;
		}

	}

	static Map<String, AssociationOverride> getAccociationOverrides(final Field field) {
		final Collection<AssociationOverride> config = new ArrayList<>();

		// Multi annotation
		final AssociationOverrides multiOverride = field.getAnnotation(AssociationOverrides.class);
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		final AssociationOverride singleOverride = field.getAnnotation(AssociationOverride.class);
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		final Map<String, AssociationOverride> attributeOverrides = new HashMap<>();
		for (final AssociationOverride override : config) {
			attributeOverrides.put(override.name(), override);
		}
		return attributeOverrides;
	}

	static Map<String, AttributeOverride> getAttributeOverrides(final Field field) {
		final Collection<AttributeOverride> config = new ArrayList<>();

		// Multi annotation
		final AttributeOverrides multiOverride = field.getAnnotation(AttributeOverrides.class);
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		final AttributeOverride singleOverride = field.getAnnotation(AttributeOverride.class);
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		final Map<String, AttributeOverride> attributeOverrides = new HashMap<>();
		for (final AttributeOverride override : config) {
			attributeOverrides.put(override.name(), override);
		}
		return attributeOverrides;
	}

	/**
	 * Indicates that the given field is written to the database.
	 *
	 * @param field
	 *            the field to check
	 * @return {@code true} if the given field is neither static, nor transient, nor generated
	 */
	static boolean isPersistentField(final Field field) {
		return !Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())
				&& field.getAnnotation(Transient.class) == null && field.getAnnotation(Formula.class) == null;
	}

	private static final SequenceGenerator DEFAULT_SEQUENCE_GENERATOR = new SequenceGenerator() {

		@Override
		public int allocationSize() {
			return 1;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return SequenceGenerator.class;
		}

		@Override
		public String catalog() {
			return "";
		}

		@Override
		public int initialValue() {
			return 1;
		}

		@Override
		public String name() {
			return "";
		}

		@Override
		public String schema() {
			return "";
		}

		@Override
		public String sequenceName() {
			return "hibernate_sequence";
		}
	};

	/** The current context. */
	private final GeneratorContext context;

	/** The represented class. */
	@NotNull
	private final Class<E> entityClass;

	/** The entity name. */
	@NotNull
	private final String entityName;

	/** The table name of the entity. */
	@NotNull
	private String table;

	/**
	 * The id in the disciminator column for this class.
	 *
	 * {@code null} if no entity superclass with {@link InheritanceType#SINGLE_TABLE} is used.
	 */
	private String dtype;

	/** The property that contains the id for the entity. */
	@NotNull
	private Property<E, ?> idProperty;

	/** The properties that make up an additional unique identifier for the entity. */
	@Nullable
	private List<SingularProperty<E, ?>> uniqueProperties;

	/** Indicates the quality of {@link #uniqueProperties}. */
	private UniquePropertyQuality uniquePropertiesQuality;

	/** Mapping from the name list of persistent properties (except the {@link #idProperty}). */
	private final Map<String, Property<E, ?>> properties = new TreeMap<>();

	/** Mapping from a {@link SequenceGenerator#name()} to the generator itself. */
	private final Map<String, SequenceGenerator> sequences = new HashMap<>();

	/**
	 * The states of written entities. Only interesting for pending updates and if the ID is not generated. Contains the
	 * ID
	 */
	private final Map<Object, GenerationState> entityStates;

	/** All attribute overriddes of this class - only used during {@link #build()}. */
	private Map<String, AttributeOverride> attributeOverrides;

	/**
	 * Creates a new description of an entity class.
	 *
	 * @param context
	 *            the current context
	 * @param entityClass
	 *            the represented class
	 */
	EntityClass(final GeneratorContext context, final Class<E> entityClass) {
		this.context = context;
		this.entityClass = entityClass;
		final String name = entityClass.getAnnotation(Entity.class).name();
		this.entityName = name.length() > 0 ? name : entityClass.getSimpleName();
		this.entityStates = context.getStates(this);
	}

	/**
	 * Reads the metadata from {@link #entityClass} and fills all other properties.
	 *
	 * Not done in the constructor to prevent endless loops.
	 */
	void build() {
		// Find the (initial) table name
		final Table tableMetadata = this.entityClass.getAnnotation(Table.class);
		this.table = tableMetadata == null || tableMetadata.name().length() == 0 ? this.entityName : tableMetadata
				.name();

		// Build the attribute overrides of the class
		buildAttributeOverrides();

		// Now build the sequences (referenced from GeneratedIdProperty)
		this.sequences.put("", DEFAULT_SEQUENCE_GENERATOR);
		buildSequences(this.entityClass);

		// Find the ID property
		buildIdProperty(this.entityClass);
		if (this.idProperty == null) {
			throw new IllegalStateException("No id found for " + this.entityClass);
		}

		// And all other properties
		buildProperties(this.entityClass);

		// And inspect unique constraints
		if (tableMetadata != null && this.uniqueProperties == null) {
			buildUniqueProperties(tableMetadata.uniqueConstraints());
		}

	}

	private void buildAttributeOverrides() {
		final Collection<AttributeOverride> config = new ArrayList<>();

		// Multi annotation
		final AttributeOverrides multiOverride = this.entityClass.getAnnotation(AttributeOverrides.class);
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		final AttributeOverride singleOverride = this.entityClass.getAnnotation(AttributeOverride.class);
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		this.attributeOverrides = new HashMap<>();
		for (final AttributeOverride override : config) {
			this.attributeOverrides.put(override.name(), override);
		}
	}

	private void buildDisciminatorType(final Class<?> c) {
		if (this.dtype == null) {
			final Inheritance inheritance = c.getAnnotation(Inheritance.class);
			final boolean isSuperclass = c != this.entityClass;
			if (inheritance == null ? isSuperclass : inheritance.strategy() != InheritanceType.TABLE_PER_CLASS) {
				this.dtype = this.entityClass.getSimpleName();
				if (isSuperclass) {
					final EntityClass<?> description = this.context.getDescription(c);
					this.table = description.table;
					if (description.dtype == null) {
						description.dtype = description.entityClass.getSimpleName();
					}
				}
			}
		}
	}

	/**
	 * Fills the {@link #idProperty}.
	 *
	 * @param c
	 *            the currently inspected class
	 */
	private void buildIdProperty(final Class<?> c) {
		// Fill properties of super classes
		if (c.getSuperclass() != null) {
			buildIdProperty(c.getSuperclass());
		}

		// Find the Entity / MappedSuperclass annotation
		if (c.getAnnotation(Entity.class) != null) {
			// As we are already here - inspect the discriminator type of the entity
			buildDisciminatorType(c);
		} else if (c.getAnnotation(MappedSuperclass.class) == null) {
			return;
		}

		// And now find the id property of this class
		for (final Field field : c.getDeclaredFields()) {
			if (field.getAnnotation(EmbeddedId.class) != null) {
				this.idProperty = new EmbeddedProperty<>(this, field);
			} else if (field.getAnnotation(Id.class) != null) {
				if (field.getAnnotation(GeneratedValue.class) != null) {
					registerSequence(field.getAnnotation(SequenceGenerator.class));
					this.idProperty = new GeneratedIdProperty<>(this, field, getColumnAnnotation(field));
				} else {
					this.idProperty = buildProperty(field, getColumnAnnotation(field), null);
				}
			}
		}

	}

	/**
	 * Fills the {@link #properties}.
	 *
	 * @param c
	 *            the currently inspected class
	 */
	private void buildProperties(final Class<?> c) {
		// Fill properties of super classes
		if (c.getSuperclass() != null) {
			buildProperties(c.getSuperclass());
		}

		// And now fill the properties of this class
		if (c.getAnnotation(MappedSuperclass.class) != null || c.getAnnotation(Entity.class) != null) {
			for (final Field field : c.getDeclaredFields()) {
				if (field.getAnnotation(EmbeddedId.class) == null && field.getAnnotation(Id.class) == null) {
					final Property<E, ?> property = buildProperty(field, getColumnAnnotation(field), null);
					if (property != null) {
						this.properties.put(field.getName(), property);
						if (property instanceof SingularProperty) {
							buildUniqueProperty((SingularProperty<E, ?>) property);
						}
					}
				}
			}
		}

	}

	<X> Property<X, ?> buildProperty(final Field field, final Column columnMetadata, final AssociationOverride override) {
		if (isPersistentField(field)) {
			if (CollectionProperty.isCollectionField(field)) {
				return new CollectionProperty<>(this, field, override);
			} else if (MapProperty.isMapField(field)) {
				return new MapProperty<>(this, field, override);
			} else if (EntityProperty.isEntityField(field)) {
				return new EntityProperty<>(this.context, field, override);
			} else if (field.getAnnotation(Embedded.class) != null) {
				return new EmbeddedProperty<>(this, field);
			} else {
				return new PrimitiveProperty<>(this.context, this.table, field, columnMetadata);
			}
		}
		return null;
	}

	/**
	 * Fills the {@link #sequences}.
	 *
	 * @param c
	 *            the currently inspected class
	 */
	private void buildSequences(final Class<?> c) {
		// First find sequences of super classes
		if (c.getSuperclass() != null) {
			buildSequences(c.getSuperclass());
		}

		registerSequence(c.getAnnotation(SequenceGenerator.class));

	}

	private void buildUniqueProperties(final UniqueConstraint[] uniqueConstraints) {
		for (final UniqueConstraint constraint : uniqueConstraints) {
			if (constraint.columnNames().length <= this.context.getMaxUniqueProperties()) {
				instpectUniqueConstraint(constraint);
			}
		}
	}

	private void buildUniqueProperty(final SingularProperty<E, ?> property) {
		if (this.context.getMaxUniqueProperties() > 0) {
			final Column column = property.getField().getAnnotation(Column.class);
			if (column != null && column.unique()) {
				final UniquePropertyQuality propertyQuality = UniquePropertyQuality.getMatchingQuality(property);
				if (propertyQuality != null && isBetterUniquePropertyQuality(propertyQuality)) {
					this.uniquePropertiesQuality = propertyQuality;
					this.uniqueProperties = Collections
							.<SingularProperty<E, ?>> singletonList((SingularProperty<E, ?>) property);
				}
			}
		}
	}

	/**
	 * Marks an entity as written and creates any pending update / insert statements.
	 *
	 * @param entity
	 *            the entity that exists now in the database
	 * @return all statements that have to be written now
	 */
	public List<EntityStatement> createPostInsertStatements(final E entity) {
		final GenerationState oldState;
		if (this.idProperty instanceof GeneratedIdProperty) {
			final GeneratedIdProperty<E> generatedIdProperty = (GeneratedIdProperty<E>) this.idProperty;
			generatedIdProperty.postInsert(entity);
			oldState = this.entityStates.remove(new EntityId(entity));
		} else {
			oldState = this.entityStates.put(getStateId(entity), GenerationState.PERSISTED);
		}
		if (oldState instanceof PendingState) {
			return ((PendingState) oldState).generatePendingStatements(entity);
		}
		return Collections.emptyList();
	}

	private Column getColumnAnnotation(final Field field) {
		final AttributeOverride override = this.attributeOverrides.get(field.getName());
		return override != null ? override.column() : field.getAnnotation(Column.class);
	}

	/**
	 * Creates an expression that references the id of an entity of this class.
	 *
	 * @param entity
	 *            the entity
	 * @param idField
	 *            the field that contains the id, only interesting if the id is an {@link EmbeddedProperty}
	 * @param whereExpression
	 *            indicates that the reference is used in a "where" statement
	 * @return the expression - either by using the {@link #getUniqueProperties() unique properties} or the
	 *         {@link #getIdProperty() id} of the entity
	 */
	public String getEntityReference(final E entity, final String idField, final boolean whereExpression) {
		if (this.idProperty instanceof GeneratedIdProperty) {
			return getGeneratedIdReference(entity, whereExpression);
		}
		Property<E, ?> property = this.idProperty;
		if (this.idProperty instanceof EmbeddedProperty) {
			final Map<String, ?> embeddedProperties = ((EmbeddedProperty<E, ?>) this.idProperty)
					.getEmbeddedProperties();
			if (idField == null) {
				if (embeddedProperties.size() > 1) {
					throw new IllegalStateException("Missing MapsId annotation for access to " + this.idProperty);
				}
				property = (Property<E, ?>) embeddedProperties.values().iterator().next();
			} else {
				property = (Property<E, ?>) embeddedProperties.get(idField);
				if (property == null) {
					throw new IllegalStateException("MapsId reference " + idField + " not found in " + this.idProperty);
				}
			}
		}
		final String expression = property.getExpression(entity, whereExpression);
		if (expression == null) {
			throw new IllegalStateException("Can't find any id in " + this.idProperty + " for " + entity);
		}
		return expression;
	}

	private String getGeneratedIdReference(final E entity, final boolean whereExpression) {
		final GeneratedIdProperty<E> generatedIdProperty = (GeneratedIdProperty<E>) this.idProperty;
		if (!generatedIdProperty.isReference(entity) && this.uniqueProperties != null) {
			// Check to write "currval" of sequence if we just have written the same value
			if (this.context.isPreferSequenceCurentValue()
					&& generatedIdProperty.getGenerator() != null
					&& this.context.getCurrentValue(generatedIdProperty.getGenerator()).equals(
							generatedIdProperty.getValue(entity))) {
				return generatedIdProperty.getExpression(entity, whereExpression);
			}

			// Check to write the reference with the unique properties
			final StringBuilder condition = new StringBuilder();
			for (final SingularProperty<E, ?> property : this.uniqueProperties) {
				final String expression = property.getPredicate(entity);
				if (expression == null) {
					// At least one required property is null -> use the id
					return generatedIdProperty.getExpression(entity, whereExpression);
				}
				if (condition.length() > 0) {
					condition.append(" AND ");
				}
				condition.append(expression);
			}
			if (this.dtype != null) {
				condition.append(" AND DTYPE='").append(this.dtype).append('\'');
			}
			return "(SELECT " + generatedIdProperty.getColumn() + " FROM " + this.table + " WHERE " + condition + ')';
		}
		return generatedIdProperty.getExpression(entity, whereExpression);
	}

	/**
	 * Resolves the column for the {@link #idProperty id property}.
	 *
	 * @param required
	 *            indicates to throw a {@link IllegalStateException} if the id property is not a
	 *            {@link SingularProperty}
	 * @return the column or {@code null} if the id property is not a {@link SingularProperty} and {@code required} is
	 *         {@code false}
	 */
	public String getIdColumn(final boolean required) {
		if (this.idProperty instanceof SingularProperty) {
			return ((SingularProperty<E, ?>) this.idProperty).getColumn();
		}
		if (required) {
			throw new IllegalStateException("ID is not a singular property for " + this.entityClass);
		}
		return null;
	}

	/**
	 * Resolves the column for the {@link #getIdProperty() id property} of this entity class.
	 *
	 * @param field
	 *            the referencing field (for evaluating annotations)
	 *
	 * @return the column name
	 * @throws IllegalStateException
	 *             if the id property is not singular and no MapsId is given
	 */
	final String getIdColumn(final Field field) {
		if (this.idProperty instanceof SingularProperty) {
			return ((SingularProperty<?, ?>) this.idProperty).getColumn();
		}
		if (this.idProperty instanceof EmbeddedProperty) {
			final MapsId mapsId = field.getAnnotation(MapsId.class);
			if (mapsId != null && mapsId.value().length() > 0) {
				((EmbeddedProperty<E, ?>) this.idProperty).getEmbeddedProperties().get(mapsId.value());
			}
			throw new IllegalStateException(field + " misses MapId annotation");
		}
		throw new IllegalStateException(field + " does not reference an ID column in " + this.entityClass);
	}

	/**
	 * Finds the id for the given entity, for look up in the {@link #entityStates}.
	 *
	 * @param entity
	 *            the entity
	 * @return the ID to use as key
	 */
	private Object getStateId(final E entity) {
		if (this.idProperty instanceof GeneratedIdProperty) {
			return new EntityId(entity);
		}
		final Object id = this.idProperty.getValue(entity);
		if (id == null) {
			throw new IllegalArgumentException("Missing id for entity of type " + this.entityClass + ": " + entity);
		}
		return id;
	}

	private void instpectUniqueConstraint(final UniqueConstraint constraint) {
		UniquePropertyQuality currentQuality = UniquePropertyQuality.onlyRequiredPrimitives;
		final List<SingularProperty<E, ?>> uniques = new ArrayList<>();
		final String[] columnNames = constraint.columnNames();
		for (final String columnName : columnNames) {
			for (final Property<E, ?> property : this.properties.values()) {
				if (property instanceof SingularProperty) {
					final SingularProperty<E, ?> singularProperty = (SingularProperty<E, ?>) property;
					if (columnName.equals(singularProperty.getColumn())) {
						final UniquePropertyQuality quality = UniquePropertyQuality.getMatchingQuality(property);
						if (quality != null) {
							if (quality.ordinal() > currentQuality.ordinal()) {
								currentQuality = quality;
							}
							uniques.add(singularProperty);
						}
					}
				}
			}
		}
		if (uniques.size() == columnNames.length && isBetterUniquePropertyQuality(currentQuality)) {
			this.uniqueProperties = uniques;
			this.uniquePropertiesQuality = currentQuality;
		}
	}

	private boolean isBetterUniquePropertyQuality(final UniquePropertyQuality foundQuality) {
		return (this.uniquePropertiesQuality == null || this.uniquePropertiesQuality.ordinal() > foundQuality.ordinal())
				&& foundQuality.ordinal() <= this.context.getUniquePropertyQuality().ordinal();
	}

	/**
	 * Indicates that the given entity needs to be written.
	 *
	 * @param entity
	 *            the entity to check
	 * @return {@code true} if the entity was neither written, nor exists in the database
	 */
	public boolean isNew(final E entity) {
		if (this.idProperty instanceof GeneratedIdProperty) {
			return ((GeneratedIdProperty<E>) this.idProperty).isNew(entity);
		}
		return this.entityStates.get(getStateId(entity)) != GenerationState.PERSISTED;
	}

	/**
	 * Marks a entity reference, where we don't know the ID in the database.
	 *
	 * @param entity
	 *            the entity to mark
	 */
	public void markExistingEntity(final E entity) {
		if (this.idProperty instanceof GeneratedIdProperty) {
			((GeneratedIdProperty<E>) this.idProperty).markReference(entity);
			this.entityStates.remove(new EntityId(entity));
		} else {
			this.entityStates.put(getStateId(entity), GenerationState.PERSISTED);
		}
	}

	/**
	 * Marks an update that is necessary when an entity is written (in the future).
	 *
	 * @param pendingEntity
	 *            the entity that needs to be written, before the update can take place
	 * @param entityToUpdate
	 *            the entity to update
	 * @param propertyToUpdate
	 *            the property of the entity to update
	 * @param arguments
	 *            additional arguments
	 */
	public <V> void markPendingUpdates(final E pendingEntity, final V entityToUpdate,
			final Property<V, ?> propertyToUpdate, final Object... arguments) {
		final Object id = getStateId(pendingEntity);
		final GenerationState state = this.entityStates.get(id);
		PendingState pendingState;
		if (state instanceof PendingState) {
			pendingState = (PendingState) state;
		} else {
			pendingState = new PendingState();
			this.entityStates.put(id, pendingState);
		}
		pendingState.addPendingUpdate(entityToUpdate, propertyToUpdate, arguments);
	}

	private void registerSequence(final SequenceGenerator generator) {
		if (generator != null) {
			this.sequences.put(generator.name(), generator);
		}
	}

	@Override
	public String toString() {
		return this.entityClass.getName();
	}
}
