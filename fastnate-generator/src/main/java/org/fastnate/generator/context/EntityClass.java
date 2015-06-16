package org.fastnate.generator.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.persistence.Access;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang.StringUtils;
import org.fastnate.generator.context.GenerationState.PendingState;
import org.fastnate.generator.statements.EntityStatement;

/**
 * Describes the DB relevant metadata of an {@link Entity entity class}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The described class
 */
@Getter
public final class EntityClass<E> {

	/**
	 * Helper object to use as key in the state map, if we have a {@link GeneratedIdProperty}.
	 *
	 * As the hashcode of entities in some implementations changes if the id changes, we can't use the entity as key
	 * itself.
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

	static Map<String, AssociationOverride> getAccociationOverrides(final AttributeAccessor attribute) {
		final Collection<AssociationOverride> config = new ArrayList<>();

		// Multi annotation
		final AssociationOverrides multiOverride = attribute.getAnnotation(AssociationOverrides.class);
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		final AssociationOverride singleOverride = attribute.getAnnotation(AssociationOverride.class);
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		final Map<String, AssociationOverride> attributeOverrides = new HashMap<>();
		for (final AssociationOverride override : config) {
			attributeOverrides.put(override.name(), override);
		}
		return attributeOverrides;
	}

	static Map<String, AttributeOverride> getAttributeOverrides(final AttributeAccessor attribute) {
		final Collection<AttributeOverride> config = new ArrayList<>();

		// Multi annotation
		final AttributeOverrides multiOverride = attribute.getAnnotation(AttributeOverrides.class);
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		final AttributeOverride singleOverride = attribute.getAnnotation(AttributeOverride.class);
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		final Map<String, AttributeOverride> attributeOverrides = new HashMap<>();
		for (final AttributeOverride override : config) {
			attributeOverrides.put(override.name(), override);
		}
		return attributeOverrides;
	}

	/** Contains the default values for a sequence generator, if none is given. */
	private static final SequenceGenerator DEFAULT_SEQUENCE_GENERATOR = AnnotationDefaults
			.create(SequenceGenerator.class);

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

	/** The type of access that is used for the properties (field access or bean property access). */
	private AccessStyle accessStyle;

	/**
	 * The inheritance type of this class.
	 *
	 * {@code null} if no subclass is known and no inhertance is indicated (using one of the inheritance annotations).
	 */
	private InheritanceType inheritanceType;

	/**
	 * Only used during {@link #build} to find the joinedParentClass even if that one references our class.
	 */
	private Class<? super E> parentEntityClass;

	/**
	 * The description of the next entity class that has {@link InheritanceType#JOINED}, if any.
	 */
	private EntityClass<? super E> joinedParentClass;

	/**
	 * The description of the entity class that is the root of the current inheritance hierarchy.
	 */
	private EntityClass<? super E> hierarchyRoot;

	/**
	 * The SQL expression that is used for the disciminator column of this class.
	 *
	 * {@code null} if no discriminator is used
	 */
	private String discriminator;

	/**
	 * The column for {@link #discriminator}.
	 *
	 * {@code null} if this class does not belong to a table that contains a discriminator column
	 */
	private String discriminatorColumn;

	/**
	 * The column that contains the id of this entity if {@link #joinedParentClass} is not {@code null}.
	 */
	private String primaryKeyJoinColumn;

	/**
	 * The property that contains the id for the entity.
	 *
	 * If a {@link #joinedParentClass} exists, this property is the id property of the parent class.
	 */
	@Nullable
	private Property<? super E, ?> idProperty;

	/** The properties that make up an additional unique identifier for the entity. */
	@Nullable
	private List<SingularProperty<E, ?>> uniqueProperties;

	/** Indicates the quality of {@link #uniqueProperties}. */
	private UniquePropertyQuality uniquePropertiesQuality;

	/**
	 * Mapping from the name list of persistent properties (except the {@link #idProperty}) and properties from
	 * {@link #joinedParentClass}.
	 */
	private final Map<String, Property<E, ?>> properties = new TreeMap<>();

	/** All properties of this entity, including {@link #idProperty} and properties from {@link #joinedParentClass}. */
	private final List<Property<? super E, ?>> allProperties = new ArrayList<>();

	/** Mapping from a {@link SequenceGenerator#name()} to the generator itself. */
	private final Map<String, SequenceGenerator> sequences = new HashMap<>();

	/** The states of written entities. Only interesting for pending updates and if the ID is not generated. */
	private final Map<Object, GenerationState> entityStates;

	/** All attribute overriddes of this class and the parent classes. */
	private final Map<String, AttributeOverride> attributeOverrides = new HashMap<>();

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
	 * Not done in the constructor to prevent endless loops (when we reference an entity class that references us).
	 */
	void build() {
		// Find the (initial) table name
		final Table tableMetadata = this.entityClass.getAnnotation(Table.class);
		this.table = tableMetadata == null || tableMetadata.name().length() == 0 ? this.entityName : tableMetadata
				.name();

		// Build the attribute overrides of this class
		buildAttributeOverrides(this.entityClass);

		// Now build the sequences (referenced from GeneratedIdProperty)
		this.sequences.put("", DEFAULT_SEQUENCE_GENERATOR);
		buildSequences(this.entityClass);

		// Build the inheritance and discriminator properties
		buildInheritance();

		// Build the discriminator, if necessary
		buildDiscriminator();

		// Find the ID property unless we have a joined parent class - which contains our id
		if (this.joinedParentClass == null) {
			buildIdProperty(this.entityClass);
			if (this.idProperty == null) {
				throw new IllegalStateException("No id found for " + this.entityClass);
			}
			this.allProperties.add(this.idProperty);

			// Add all other properties
			buildProperties(this.entityClass, Object.class);
		} else {
			this.idProperty = this.joinedParentClass.getIdProperty();
			this.allProperties.add(this.idProperty);
			this.allProperties.add(this.joinedParentClass.getIdProperty());

			// Add all other properties
			buildProperties(this.entityClass, this.joinedParentClass.entityClass);
		}

		// And inspect unique constraints
		if (tableMetadata != null && this.uniqueProperties == null) {
			buildUniqueProperties(tableMetadata.uniqueConstraints());
		}

	}

	private void buildAttributeOverrides(final Class<? super E> inspectedClass) {
		if (inspectedClass.getSuperclass() != null) {
			buildAttributeOverrides(inspectedClass.getSuperclass());
		}

		// Multi annotation
		final AttributeOverrides multiOverride = this.entityClass.getAnnotation(AttributeOverrides.class);
		if (multiOverride != null) {
			for (final AttributeOverride override : multiOverride.value()) {
				this.attributeOverrides.put(override.name(), override);
			}
		}

		// Single annotion
		final AttributeOverride singleOverride = this.entityClass.getAnnotation(AttributeOverride.class);
		if (singleOverride != null) {
			this.attributeOverrides.put(singleOverride.name(), singleOverride);
		}
	}

	private void buildDiscriminator() {
		if (this.inheritanceType == InheritanceType.SINGLE_TABLE || this.inheritanceType == InheritanceType.JOINED) {
			final DiscriminatorColumn column = this.hierarchyRoot.entityClass.getAnnotation(DiscriminatorColumn.class);
			if (column != null || this.inheritanceType != InheritanceType.JOINED
					|| this.context.getProvider().isJoinedDiscriminatorNeeded()) {
				this.discriminatorColumn = column == null ? "DTYPE" : column.name();
				this.discriminator = buildDiscriminator(this, column);
			}
		}
	}

	private String buildDiscriminator(final EntityClass<?> c, final DiscriminatorColumn column) {
		DiscriminatorType type;
		int maxLength;
		if (column == null) {
			type = DiscriminatorType.STRING;
			final int defaultMaxLength = 31;
			maxLength = defaultMaxLength;
		} else {
			type = column.discriminatorType();
			maxLength = column.length();
		}

		final DiscriminatorValue value = this.entityClass.getAnnotation(DiscriminatorValue.class);
		if (type == DiscriminatorType.INTEGER) {
			return value == null ? String.valueOf(c.getEntityName().hashCode()) : value.value();
		}
		final String v = value == null ? c.getEntityName() : value.value();
		if (StringUtils.isEmpty(v)) {
			throw new IllegalArgumentException("Missing discriminator value for: " + c.getEntityClass());
		}
		if (type == DiscriminatorType.STRING) {
			return getContext().getDialect().quoteString(v.length() <= maxLength ? v : v.substring(0, maxLength));
		} else if (type == DiscriminatorType.CHAR) {
			return getContext().getDialect().quoteString(v.substring(0, 1));
		}
		throw new IllegalArgumentException("Unknown discriminator type: " + type);
	}

	/**
	 * Fills the {@link #idProperty}.
	 *
	 * @param c
	 *            the currently inspected class
	 */
	private void buildIdProperty(final Class<? super E> c) {
		// TODO (functional) Support @IdClass

		// Find ID properties of super classes
		if (c.getSuperclass() != null) {
			buildIdProperty(c.getSuperclass());
		}

		// Find the Entity / MappedSuperclass annotation
		if (c.isAnnotationPresent(Entity.class) || c.isAnnotationPresent(MappedSuperclass.class)) {
			// Determine the access type
			if (this.accessStyle == null) {
				final Access accessType = c.getAnnotation(Access.class);
				if (accessType != null) {
					this.accessStyle = AccessStyle.getStyle(accessType.value());
				}
			}

			// And now find the id property of this class
			if (this.accessStyle == null) {
				if (findIdProperty(AccessStyle.FIELD.getDeclaredAttributes(c))) {
					this.accessStyle = AccessStyle.FIELD;
				} else if (findIdProperty(AccessStyle.METHOD.getDeclaredAttributes(c))) {
					this.accessStyle = AccessStyle.METHOD;
				}
			} else if (this.accessStyle == AccessStyle.FIELD) {
				findIdProperty(AccessStyle.FIELD.getDeclaredAttributes(c));
			} else {
				findIdProperty(AccessStyle.METHOD.getDeclaredAttributes(c));
			}
		}
	}

	/**
	 * Determine the inheritance type and discriminator properties.
	 */
	private void buildInheritance() {
		// Check, if we've got an explicit inheritance type
		final Inheritance inheritance = this.entityClass.getAnnotation(Inheritance.class);
		if (inheritance != null) {
			this.inheritanceType = inheritance.strategy();
		}

		// Find the root of our hierarchy
		this.hierarchyRoot = this;
		findHierarchyRoot(this.entityClass.getSuperclass());

		// We scan only classes that we are about to write
		// So we don't know, that there is a subclass entity - until we find one
		// This could be to late for InheritanceType.SINGLE_TABLE - the defaault type
		// That's why we build a discriminator, if one of the inheritance annotations exists
		if (this.inheritanceType == null && this.entityClass.isAnnotationPresent(DiscriminatorColumn.class)
				|| this.entityClass.isAnnotationPresent(DiscriminatorValue.class)) {
			this.inheritanceType = InheritanceType.SINGLE_TABLE;
		}

		buildDiscriminator();
	}

	private void buildPrimaryKeyJoinColumn() {
		if (this.joinedParentClass.getIdProperty() instanceof SingularProperty) {
			final PrimaryKeyJoinColumn pkColumn = this.entityClass.getAnnotation(PrimaryKeyJoinColumn.class);
			if (pkColumn == null || StringUtils.isEmpty(pkColumn.name())) {
				this.primaryKeyJoinColumn = ((SingularProperty<? super E, ?>) this.joinedParentClass.getIdProperty())
						.getColumn();
			} else {
				this.primaryKeyJoinColumn = pkColumn.name();
			}
		} else {
			throw new IllegalArgumentException(
					"JOINED inheritance strategy is currently only supported with singular ID properties.");
		}
	}

	/**
	 * Fills the {@link #properties}.
	 *
	 * @param c
	 *            the currently inspected class
	 * @param stopClass
	 *            the class in the hierarchy to stop inspecting
	 */
	private void buildProperties(final Class<?> c, final Class<?> stopClass) {
		// Fill properties of super classes (at least until we find the joined parent class)
		if (c.getSuperclass() != null && c.getSuperclass() != stopClass) {
			buildProperties(c.getSuperclass(), stopClass);
		}

		// And now fill the properties of this class
		if (c.isAnnotationPresent(MappedSuperclass.class) || c.isAnnotationPresent(Entity.class)) {
			for (final AttributeAccessor field : this.accessStyle.getDeclaredAttributes(c)) {
				if (!field.hasAnnotation(EmbeddedId.class) && !field.hasAnnotation(Id.class)) {
					final Property<E, ?> property = buildProperty(field, getColumnAnnotation(field), null);
					if (property != null) {
						this.properties.put(field.getName(), property);
						this.allProperties.add(property);
						if (property instanceof SingularProperty) {
							buildUniqueProperty((SingularProperty<E, ?>) property);
						}
					}
				}
			}
		}

	}

	<X> Property<X, ?> buildProperty(final AttributeAccessor attribute, final Column columnMetadata,
			final AssociationOverride override) {
		if (attribute.isPersistent()) {
			if (CollectionProperty.isCollectionProperty(attribute)) {
				return new CollectionProperty<>(this, attribute, override);
			} else if (MapProperty.isMapProperty(attribute)) {
				return new MapProperty<>(this, attribute, override);
			} else if (EntityProperty.isEntityProperty(attribute)) {
				return new EntityProperty<>(this.context, attribute, override);
			} else if (attribute.hasAnnotation(Embedded.class)) {
				return new EmbeddedProperty<>(this, attribute);
			} else {
				return new PrimitiveProperty<>(this.context, this.table, attribute, columnMetadata);
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
			final Column column = property.getAttribute().getAnnotation(Column.class);
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

	private void findHierarchyRoot(final Class<? super E> inspectedClass) {
		if (inspectedClass != null) {
			if (!inspectedClass.isAnnotationPresent(Entity.class)) {
				findHierarchyRoot(inspectedClass.getSuperclass());
			} else {
				this.parentEntityClass = inspectedClass;
				final EntityClass<? super E> parentDescription = this.context.getDescription(inspectedClass);
				this.accessStyle = parentDescription.getAccessStyle();
				if (parentDescription.inheritanceType == null) {
					parentDescription.inheritanceType = InheritanceType.SINGLE_TABLE;
					parentDescription.buildDiscriminator();
				}
				if (this.inheritanceType == null) {
					this.inheritanceType = parentDescription.inheritanceType;
					this.hierarchyRoot = parentDescription.hierarchyRoot;
				} else if (parentDescription.inheritanceType != InheritanceType.TABLE_PER_CLASS) {
					this.hierarchyRoot = parentDescription.hierarchyRoot;
				}
				if (parentDescription.getInheritanceType() == InheritanceType.JOINED) {
					this.joinedParentClass = parentDescription;
					buildPrimaryKeyJoinColumn();
				} else {
					if (parentDescription.getInheritanceType() == InheritanceType.SINGLE_TABLE) {
						this.table = parentDescription.table;
					}
					this.joinedParentClass = parentDescription.joinedParentClass;
					this.primaryKeyJoinColumn = parentDescription.primaryKeyJoinColumn;
				}
			}
		}
	}

	private boolean findIdProperty(final Iterable<AttributeAccessor> declaredAttributes) {
		for (final AttributeAccessor attribute : declaredAttributes) {
			if (attribute.hasAnnotation(EmbeddedId.class)) {
				this.idProperty = new EmbeddedProperty<>(this, attribute);
				return true;
			} else if (attribute.hasAnnotation(Id.class)) {
				if (attribute.hasAnnotation(GeneratedValue.class)) {
					if (attribute.getType().isPrimitive()) {
						throw new IllegalArgumentException("Generated ID must not be of primitive type.");
					}
					registerSequence(attribute.getAnnotation(SequenceGenerator.class));
					this.idProperty = new GeneratedIdProperty<>(this, attribute, getColumnAnnotation(attribute));
				} else {
					this.idProperty = buildProperty(attribute, getColumnAnnotation(attribute), null);
				}
				return true;
			}
		}
		return false;
	}

	private Column getColumnAnnotation(final AttributeAccessor attribute) {
		final AttributeOverride override = this.attributeOverrides.get(attribute.getName());
		return override != null ? override.column() : attribute.getAnnotation(Column.class);
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
		if (this.joinedParentClass != null) {
			return this.joinedParentClass.getEntityReference(entity, idField, whereExpression);
		}
		if (this.idProperty instanceof GeneratedIdProperty) {
			return getGeneratedIdReference(entity, whereExpression);
		}
		Property<? super E, ?> property = this.idProperty;
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
			if (this.discriminator != null) {
				condition.append(" AND ").append(this.discriminatorColumn).append(" = ").append(this.discriminator);
			}
			return "(SELECT " + generatedIdProperty.getColumn() + " FROM " + this.table + " WHERE " + condition + ')';
		}
		return generatedIdProperty.getExpression(entity, whereExpression);
	}

	/**
	 * Resolves the column for the {@link #getIdProperty() id property} of this entity class.
	 *
	 * @param attribute
	 *            the referencing attribute (for evaluating annotations)
	 *
	 * @return the column name
	 * @throws IllegalStateException
	 *             if the id property is not singular and no MapsId is given
	 */
	String getIdColumn(final AttributeAccessor attribute) {
		if (this.idProperty instanceof SingularProperty) {
			return ((SingularProperty<?, ?>) this.idProperty).getColumn();
		}
		if (this.idProperty instanceof EmbeddedProperty) {
			final MapsId mapsId = attribute.getAnnotation(MapsId.class);
			if (mapsId != null && mapsId.value().length() > 0) {
				((EmbeddedProperty<E, ?>) this.idProperty).getEmbeddedProperties().get(mapsId.value());
			}
			throw new IllegalStateException(attribute + " misses MapId annotation");
		}
		throw new IllegalStateException(attribute + " does not reference an ID column in " + this.entityClass);
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
