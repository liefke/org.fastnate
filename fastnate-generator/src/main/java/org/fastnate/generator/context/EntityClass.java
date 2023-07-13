package org.fastnate.generator.context;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Access;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.generator.context.GenerationState.PendingState;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PlainColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

	/**
	 * Finds all association overrides that are attached to the given field, method or class, by taking the already
	 * defined overrides into account.
	 *
	 * @param surroundingOverrides
	 *            the overrides already defined for the surrounding element
	 * @param prefix
	 *            the prefix of the element, for lookup in {@code surroundingOverrides}
	 * @param element
	 *            the annotated element
	 * @return a mapping from the name of the override to its definition, empty if neither {@link AssociationOverrides}
	 *         nor {@link AssociationOverride} are given
	 */
	static Map<String, AssociationOverride> getAccociationOverrides(
			final Map<String, AssociationOverride> surroundingOverrides, final String prefix,
			final AnnotatedElement element) {
		final AssociationOverrides multiOverride = element.getAnnotation(AssociationOverrides.class);
		final AssociationOverride singleOverride = element.getAnnotation(AssociationOverride.class);

		if (multiOverride == null && singleOverride == null && surroundingOverrides.isEmpty()) {
			return Collections.emptyMap();
		}

		final Collection<AssociationOverride> config = new ArrayList<>();

		// Multi annotation
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		final Map<String, AssociationOverride> attributeOverrides = new HashMap<>();
		for (final AssociationOverride override : config) {
			attributeOverrides.put(override.name(), override);
		}

		for (final Map.Entry<String, AssociationOverride> override : surroundingOverrides.entrySet()) {
			if (override.getKey().startsWith(prefix)) {
				attributeOverrides.put(override.getKey().substring(prefix.length()), override.getValue());
			}
		}
		return attributeOverrides;
	}

	/**
	 * Finds all {@link AttributeOverrides} that are attached to the given field, method or class, by taking the already
	 * defined overrides into account.
	 *
	 * @param surroundingOverrides
	 *            the overrides already defined for the surrounding element
	 * @param prefix
	 *            the prefix of the element, for lookup in {@code surroundingOverrides}
	 * @param element
	 *            the annotated element
	 * @return a mapping from the name of the override to its definition, empty if neither {@link AttributeOverrides}
	 *         nor {@link AttributeOverride} are given
	 */
	static Map<String, AttributeOverride> getAttributeOverrides(
			final Map<String, AttributeOverride> surroundingOverrides, final String prefix,
			final AnnotatedElement element) {
		final AttributeOverrides multiOverride = element.getAnnotation(AttributeOverrides.class);
		final AttributeOverride singleOverride = element.getAnnotation(AttributeOverride.class);

		if (multiOverride == null && singleOverride == null && surroundingOverrides.isEmpty()) {
			return surroundingOverrides;
		}

		final Collection<AttributeOverride> config = new ArrayList<>();

		// Multi annotation
		if (multiOverride != null) {
			config.addAll(Arrays.asList(multiOverride.value()));
		}

		// Single annotion
		if (singleOverride != null) {
			config.add(singleOverride);
		}

		final Map<String, AttributeOverride> attributeOverrides = new HashMap<>();
		for (final AttributeOverride override : config) {
			attributeOverrides.put(override.name(), override);
		}

		for (final Map.Entry<String, AttributeOverride> override : surroundingOverrides.entrySet()) {
			if (override.getKey().startsWith(prefix)) {
				attributeOverrides.put(override.getKey().substring(prefix.length()), override.getValue());
			}
		}
		return attributeOverrides;
	}

	private static Column getColumnAnnotation(final AttributeAccessor attribute,
			final Map<String, AttributeOverride> overrides) {
		final AttributeOverride override = overrides.get(attribute.getName());
		return override != null ? override.column() : attribute.getAnnotation(Column.class);
	}

	/** The current context. */
	private final GeneratorContext context;

	/** The represented class. */
	@Nonnull
	private final Class<E> entityClass;

	/** The JPA constructor. */
	@Nonnull
	@Getter(AccessLevel.NONE)
	private final Constructor<E> entityConstructor;

	/** The entity name. */
	@Nonnull
	private final String entityName;

	/** The main table of the entity. */
	@Nonnull
	private GeneratorTable table;

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
	private ColumnExpression discriminator;

	/**
	 * The column for {@link #discriminator}.
	 *
	 * {@code null} if this class does not belong to a table that contains a discriminator column
	 */
	private GeneratorColumn discriminatorColumn;

	/**
	 * The column that contains the id of this entity if {@link #joinedParentClass} is not {@code null}.
	 */
	private GeneratorColumn primaryKeyJoinColumn;

	/**
	 * The property that contains the id for the entity.
	 *
	 * If a {@link #joinedParentClass} exists, this property is the id property of the parent class.
	 */
	@Nullable
	private Property<? super E, ?> idProperty;

	/**
	 * The properties that make up an additional unique identifier for the entity.
	 *
	 * Depending on {@link GeneratorContext#getUniquePropertyQuality()} and
	 * {@link GeneratorContext#getMaxUniqueProperties()} this might be {@code null}, even if there is a set of
	 * {@link #allUniqueProperties available unique properties}, if the quality is not enough.
	 */
	@Nullable
	private List<SingularProperty<E, ?>> uniqueProperties;

	/** Indicates the quality of {@link #uniqueProperties}. */
	private UniquePropertyQuality uniquePropertiesQuality;

	/** All available sets of properties that make up an additional unique identifier for the entities. */
	private final List<List<SingularProperty<E, ?>>> allUniqueProperties = new ArrayList<>();

	/** Mapping from the name of all persistent properties to their description. */
	private final Map<String, Property<? super E, ?>> properties = new TreeMap<>();

	/** All properties of this entity, including {@link #idProperty} and properties from {@link #joinedParentClass}. */
	private final List<Property<? super E, ?>> allProperties = new ArrayList<>();

	/** Properties of this entity, excluding the {@link #idProperty} or properties from {@link #joinedParentClass}. */
	private final List<Property<? super E, ?>> additionalProperties = new ArrayList<>();

	/** The states of written entities. Only interesting for pending updates and if the ID is not generated. */
	private final Map<Object, GenerationState> entityStates;

	/** All attribute overriddes of this class and the parent classes. */
	private final Map<String, AttributeOverride> attributeOverrides = new HashMap<>();

	/** All association overriddes of this class and the parent classes. */
	private final Map<String, AssociationOverride> associationOverrides = new HashMap<>();

	/** The list of listeners that are informed when this class has finished to build all properties. */
	@Getter(AccessLevel.NONE)
	private List<Consumer<EntityClass<E>>> builtListeners = new ArrayList<>();

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

		try {
			// Try to find the noargs constructor
			this.entityConstructor = entityClass.getDeclaredConstructor();
			if (!Modifier.isPublic(this.entityConstructor.getModifiers())) {
				this.entityConstructor.setAccessible(true);
			}
		} catch (final NoSuchMethodException e) {
			throw new ModelException("Could not find constructor without arguments for " + entityClass);
		}
	}

	/**
	 * Reads the metadata from {@link #entityClass} and fills all other properties.
	 *
	 * Not done in the constructor to prevent endless loops (when we reference an entity class that references us).
	 */
	void build() {
		// Find the (initial) table name
		final Table tableMetadata = this.entityClass.getAnnotation(Table.class);
		this.table = this.context.resolveTable(null, tableMetadata, Table::catalog, Table::schema, Table::name,
				this.entityName);

		// Build the attribute and association overrides of this class
		buildOverrides(this.entityClass);

		// Now build the sequences (referenced from GeneratedIdProperty)
		buildGenerators(this.entityClass);

		// Build the inheritance and discriminator properties
		buildInheritance();

		// Build the discriminator, if necessary
		buildDiscriminator();

		// Find the ID property unless we have a joined parent class - which contains our id
		if (this.joinedParentClass == null) {
			buildIdProperty(this.entityClass);
			ModelException.test(this.idProperty != null, "No id found for {}", this.entityClass);
			this.properties.put(this.idProperty.getName(), this.idProperty);
			this.allProperties.add(this.idProperty);

			// Add all other properties
			buildProperties(this.entityClass, Object.class);
		} else {
			this.idProperty = this.joinedParentClass.getIdProperty();
			this.properties.putAll(this.joinedParentClass.getProperties());
			this.allProperties.addAll(this.joinedParentClass.getAllProperties());

			// Add all other properties
			buildProperties(this.entityClass, this.joinedParentClass.entityClass);
		}

		// Inspect unique constraints
		if (tableMetadata != null) {
			buildUniqueProperties(tableMetadata.uniqueConstraints());
		}

		// Sort properties by name to have "stable" SQL (which looks the same between different runs)
		this.allProperties.sort(Comparator.comparing(Property::getName));
		this.additionalProperties.sort(Comparator.comparing(Property::getName));

		// Inform any listners
		this.builtListeners.stream().forEach(listener -> listener.accept(this));
		this.builtListeners = null;
	}

	private void buildDiscriminator() {
		if (this.inheritanceType == InheritanceType.SINGLE_TABLE || this.inheritanceType == InheritanceType.JOINED) {
			final DiscriminatorColumn column = this.hierarchyRoot.entityClass.getAnnotation(DiscriminatorColumn.class);
			if (column != null || this.inheritanceType != InheritanceType.JOINED
					|| this.context.getProvider().isJoinedDiscriminatorNeeded()) {
				this.discriminatorColumn = this.table.resolveColumn(column == null ? "DTYPE" : column.name());
				this.discriminator = buildDiscriminator(this, column);
			}
		}
	}

	private ColumnExpression buildDiscriminator(final EntityClass<?> c, final DiscriminatorColumn column) {
		final DiscriminatorType type;
		final int maxLength;
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
			return PrimitiveColumnExpression.create(
					value == null ? c.getEntityName().hashCode() : Integer.parseInt(value.value()),
					getContext().getDialect());
		}
		final String v = value == null ? c.getEntityName() : value.value();
		if (StringUtils.isEmpty(v)) {
			throw new IllegalArgumentException("Missing discriminator value for: " + c.getEntityClass());
		}
		if (type == DiscriminatorType.STRING) {
			return PrimitiveColumnExpression.create(v.length() <= maxLength ? v : v.substring(0, maxLength),
					getContext().getDialect());
		} else if (type == DiscriminatorType.CHAR) {
			return PrimitiveColumnExpression.create(v.substring(0, 1), getContext().getDialect());
		}
		throw new IllegalArgumentException("Unknown discriminator type: " + type);
	}

	/**
	 * Fills the generators of the {@link #context}.
	 *
	 * @param c
	 *            the currently inspected class
	 */
	private void buildGenerators(final Class<?> c) {
		// First find sequences of super classes
		if (c.getSuperclass() != null) {
			buildGenerators(c.getSuperclass());
		}

		this.context.registerGenerators(c, this.table);
	}

	/**
	 * Fills the {@link #idProperty}.
	 *
	 * @param c
	 *            the currently inspected class
	 */
	private void buildIdProperty(final Class<? super E> c) {
		// TODO (Issue #2) Support @IdClass

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
				if (findIdProperty(AccessStyle.FIELD.getDeclaredAttributes(c, this.entityClass))) {
					this.accessStyle = AccessStyle.FIELD;
				} else if (findIdProperty(AccessStyle.METHOD.getDeclaredAttributes(c, this.entityClass))) {
					this.accessStyle = AccessStyle.METHOD;
				}
			} else {
				findIdProperty(this.accessStyle.getDeclaredAttributes(c, this.entityClass));
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
		// This could be to late for InheritanceType.SINGLE_TABLE - the default type
		// That's why we build a discriminator, if one of the inheritance annotations exists
		if (this.inheritanceType == null && (this.entityClass.isAnnotationPresent(DiscriminatorColumn.class)
				|| this.entityClass.isAnnotationPresent(DiscriminatorValue.class))) {
			this.inheritanceType = InheritanceType.SINGLE_TABLE;
		}

		buildDiscriminator();
	}

	private void buildOverrides(final Class<? super E> inspectedClass) {
		if (inspectedClass.getSuperclass() != null) {
			buildOverrides(inspectedClass.getSuperclass());
		}

		// Add the attribute and association overrides of this class
		this.attributeOverrides.putAll(getAttributeOverrides(Collections.emptyMap(), "", inspectedClass));
		this.associationOverrides.putAll(getAccociationOverrides(Collections.emptyMap(), "", inspectedClass));
	}

	private void buildPrimaryKeyJoinColumn() {
		if (this.joinedParentClass.getIdProperty() instanceof SingularProperty) {
			final PrimaryKeyJoinColumn pkColumn = this.entityClass.getAnnotation(PrimaryKeyJoinColumn.class);
			final String columnName;
			if (pkColumn == null || StringUtils.isEmpty(pkColumn.name())) {
				columnName = ((SingularProperty<? super E, ?>) this.joinedParentClass.getIdProperty()).getColumn()
						.getName();
			} else {
				columnName = pkColumn.name();
			}
			this.primaryKeyJoinColumn = this.table.resolveColumn(columnName);
		} else {
			throw new ModelException(
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
	private void buildProperties(final Class<? super E> c, final Class<? super E> stopClass) {
		// Fill properties of super classes (at least until we find the joined parent class)
		if (c.getSuperclass() != null && c.getSuperclass() != stopClass) {
			buildProperties(c.getSuperclass(), stopClass);
		}

		// And now fill the properties of this class
		if (c.isAnnotationPresent(MappedSuperclass.class) || c.isAnnotationPresent(Entity.class)) {
			for (final AttributeAccessor attribute : this.accessStyle.getDeclaredAttributes(c, this.entityClass)) {
				if (!attribute.isAnnotationPresent(EmbeddedId.class) && !attribute.isAnnotationPresent(Id.class)) {
					final Property<E, ?> property = buildProperty(this.table, attribute, this.attributeOverrides,
							this.associationOverrides);
					if (property != null) {
						this.properties.put(attribute.getName(), property);
						this.allProperties.add(property);
						this.additionalProperties.add(property);
						if (property instanceof SingularProperty) {
							buildUniqueProperty((SingularProperty<E, ?>) property);
						}
					}
				}
			}
		}
	}

	/**
	 * Builds the property for the given attribute.
	 *
	 * @param propertyTable
	 *            the table of the new property (if it is not a collection property)
	 * @param attribute
	 *            the attribute to inspect
	 * @param surroundingAttributeOverrides
	 *            the overrides defined for the surrounding element
	 * @param surroundingAssociationOverrides
	 *            the overrides defined for the surrounding element
	 * @return the property that represents the attribute or {@code null} if not persistent
	 */
	<X> Property<X, ?> buildProperty(final GeneratorTable propertyTable, final AttributeAccessor attribute,
			final Map<String, AttributeOverride> surroundingAttributeOverrides,
			final Map<String, AssociationOverride> surroundingAssociationOverrides) {
		if (!attribute.isPersistent()) {
			return null;
		}
		if (CollectionProperty.isCollectionProperty(attribute)) {
			ModelException.test(propertyTable == this.table, "Unsupported nesting of collection property {}",
					attribute);
			return new CollectionProperty<>(this, attribute, surroundingAssociationOverrides.get(attribute.getName()),
					surroundingAttributeOverrides.get(attribute.getName()));
		}
		if (MapProperty.isMapProperty(attribute)) {
			ModelException.test(propertyTable == this.table, "Unsupported nesting of map property {}", attribute);
			return new MapProperty<>(this, attribute, surroundingAssociationOverrides.get(attribute.getName()),
					surroundingAttributeOverrides.get(attribute.getName()));
		}
		if (EntityProperty.isEntityProperty(attribute)) {
			return new EntityProperty<>(this.context, propertyTable, attribute,
					surroundingAssociationOverrides.get(attribute.getName()));
		}
		if (attribute.isAnnotationPresent(Embedded.class)) {
			return new EmbeddedProperty<>(this, propertyTable, attribute, surroundingAttributeOverrides,
					surroundingAssociationOverrides);
		}

		final Column columnMetadata = getColumnAnnotation(attribute, surroundingAttributeOverrides);
		if (attribute.isAnnotationPresent(Version.class)) {
			return new VersionProperty<>(this.context, propertyTable, attribute, columnMetadata);
		}
		return new PrimitiveProperty<>(this.context, propertyTable, attribute, columnMetadata);
	}

	private void buildUniqueProperties(final UniqueConstraint[] uniqueConstraints) {
		for (final UniqueConstraint constraint : uniqueConstraints) {
			inspectUniqueConstraint(constraint);
		}
	}

	private void buildUniqueProperty(final SingularProperty<E, ?> property) {
		final boolean unique;
		final Column column = property.getAttribute().getAnnotation(Column.class);
		if (column != null && column.unique()) {
			unique = true;
		} else {
			final OneToOne oneToOne = property.getAttribute().getAnnotation(OneToOne.class);
			if (oneToOne != null) {
				unique = StringUtils.isEmpty(oneToOne.mappedBy());
			} else {
				final JoinColumn joinColumn = property.getAttribute().getAnnotation(JoinColumn.class);
				unique = joinColumn != null && joinColumn.unique();
			}
		}
		if (unique) {
			final List<SingularProperty<E, ?>> uniques = Collections.singletonList((SingularProperty<E, ?>) property);
			this.allUniqueProperties.add(uniques);
			if (this.context.getMaxUniqueProperties() > 0) {
				final UniquePropertyQuality propertyQuality = UniquePropertyQuality.getMatchingQuality(property);
				if (propertyQuality != null && isBetterUniquePropertyQuality(propertyQuality)) {
					this.uniquePropertiesQuality = propertyQuality;
					this.uniqueProperties = uniques;
				}
			}
		}
	}

	/**
	 * Marks an entity as written and creates any pending update / insert statements.
	 *
	 * @param entity
	 *            the entity that exists now in the database
	 * @param writer
	 *            the target of the new statements
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void createPostInsertStatements(final E entity, final StatementsWriter writer) throws IOException {
		if (this.joinedParentClass == null) {
			final GenerationState oldState;
			if (this.idProperty instanceof GeneratedIdProperty) {
				final GeneratedIdProperty<E, ?> generatedIdProperty = (GeneratedIdProperty<E, ?>) this.idProperty;
				generatedIdProperty.postInsert(entity);
				if (generatedIdProperty.isPrimitive() && generatedIdProperty.getValue(entity).longValue() == 0) {
					// Mark the first entity of the generation as persisted,
					// as we can't distinguish it from new instances otherwise
					oldState = this.entityStates.put(new EntityId(entity), GenerationState.PERSISTED);
				} else {
					oldState = this.entityStates.remove(new EntityId(entity));
				}
			} else {
				oldState = this.entityStates.put(getStateId(entity), GenerationState.PERSISTED);
			}
			if (oldState instanceof PendingState) {
				((PendingState) oldState).writePendingStatements(writer, entity);
			}
		}
	}

	private void findHierarchyRoot(final Class<? super E> inspectedClass) {
		if (inspectedClass != null) {
			if (!inspectedClass.isAnnotationPresent(Entity.class)) {
				findHierarchyRoot(inspectedClass.getSuperclass());
			} else {
				this.parentEntityClass = inspectedClass;
				final EntityClass<? super E> parentDescription = this.context.getDescription(inspectedClass);
				this.accessStyle = parentDescription.accessStyle;
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
				if (parentDescription.inheritanceType == InheritanceType.JOINED) {
					this.joinedParentClass = parentDescription;
					buildPrimaryKeyJoinColumn();
				} else {
					if (parentDescription.inheritanceType == InheritanceType.SINGLE_TABLE) {
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
			if (attribute.isAnnotationPresent(EmbeddedId.class)) {
				this.idProperty = new EmbeddedProperty<>(this, this.table, attribute, this.attributeOverrides,
						this.associationOverrides);
				return true;
			} else if (attribute.isAnnotationPresent(Id.class)) {
				if (attribute.isAnnotationPresent(GeneratedValue.class)) {
					this.context.registerGenerators(attribute, this.table);
					this.idProperty = new GeneratedIdProperty<>(this, attribute,
							getColumnAnnotation(attribute, this.attributeOverrides));
				} else {
					this.idProperty = buildProperty(this.table, attribute, this.attributeOverrides,
							this.associationOverrides);
				}
				return true;
			}
		}
		return false;
	}

	private SingularProperty<E, ?> findPropertyByColumnName(final String columnName) {
		for (final Property<? super E, ?> property : this.properties.values()) {
			if (property instanceof SingularProperty) {
				final SingularProperty<E, ?> singularProperty = (SingularProperty<E, ?>) property;
				if (singularProperty.getColumn() != null && columnName.equals(singularProperty.getColumn().getName())) {
					return singularProperty;
				}
			}
		}
		return null;
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
	public ColumnExpression getEntityReference(final E entity, final String idField, final boolean whereExpression) {
		if (this.joinedParentClass != null) {
			return this.joinedParentClass.getEntityReference(entity, idField, whereExpression);
		}
		Property<? super E, ?> property = this.idProperty;
		if (property instanceof GeneratedIdProperty) {
			if (this.context.isWriteRelativeIds()) {
				return getGeneratedIdReference(entity, whereExpression);
			}
			return property.getExpression(entity, whereExpression);
		}
		if (property instanceof EmbeddedProperty) {
			final Map<String, ?> embeddedProperties = ((EmbeddedProperty<E, ?>) this.idProperty)
					.getEmbeddedProperties();
			if (idField == null) {
				ModelException.test(embeddedProperties.size() != 1, "Missing MapsId annotation for access to {}",
						this.idProperty);
				property = (Property<E, ?>) embeddedProperties.values().iterator().next();
			} else {
				property = (Property<E, ?>) embeddedProperties.get(idField);
				ModelException.test(property != null, "MapsId reference {} not found in {}", idField, this.idProperty);
			}
		}
		@SuppressWarnings("null")
		final ColumnExpression expression = property.getExpression(entity, whereExpression);
		ModelException.test(expression != null, "Can't find any id for {} in property '{}'", this.idProperty, entity);
		return expression;
	}

	private ColumnExpression getGeneratedIdReference(final E entity, final boolean whereExpression) {
		final GeneratedIdProperty<E, ?> generatedIdProperty = (GeneratedIdProperty<E, ?>) this.idProperty;
		if (!generatedIdProperty.isReference(entity) && this.uniqueProperties != null) {
			// Check to write "currval" of sequence if we just have written the same value
			if (this.context.isPreferSequenceCurentValue()) {
				final IdGenerator generator = generatedIdProperty.getGenerator();
				if (generator instanceof SequenceIdGenerator
						&& generator.getCurrentValue() == generatedIdProperty.getValue(entity).longValue()) {
					return generatedIdProperty.getExpression(entity, whereExpression);
				}
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
			return new PlainColumnExpression(
					"(SELECT " + generatedIdProperty.getColumn() + " FROM " + this.table + " WHERE " + condition + ')');
		}
		return generatedIdProperty.getExpression(entity, whereExpression);
	}

	/**
	 * Resolves the column for the {@code id property} of this entity class.
	 *
	 * @param attribute
	 *            the referencing attribute (for evaluating annotations)
	 *
	 * @return the column name
	 * @throws IllegalStateException
	 *             if the id property is not singular and no MapsId is given
	 */
	GeneratorColumn getIdColumn(final AttributeAccessor attribute) {
		// Maybe we are still building the hierarchy
		Class<? super E> parentClass = this.parentEntityClass;
		while (this.idProperty == null && parentClass != null) {
			final EntityClass<? super E> parentDescription = this.context.getDescription(parentClass);
			this.idProperty = parentDescription.idProperty;
			parentClass = parentDescription.parentEntityClass;
		}

		if (this.idProperty instanceof SingularProperty) {
			return ((SingularProperty<?, ?>) this.idProperty).getColumn();
		}
		if (this.idProperty instanceof EmbeddedProperty) {
			final MapsId mapsId = attribute.getAnnotation(MapsId.class);
			if (mapsId != null && mapsId.value().length() > 0) {
				final Property<?, ?> property = ((EmbeddedProperty<E, ?>) this.idProperty).getEmbeddedProperties()
						.get(mapsId.value());
				if (property instanceof SingularProperty) {
					return ((SingularProperty<?, ?>) this.idProperty).getColumn();
				}
			}
			throw new ModelException(attribute + " misses MapId for a singular property in " + this.entityClass);
		}

		throw new ModelException(attribute + " does not reference an ID column in " + this.entityClass);
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

	private void inspectUniqueConstraint(final UniqueConstraint constraint) {
		UniquePropertyQuality currentQuality = UniquePropertyQuality.onlyRequiredPrimitives;
		final String[] columnNames = constraint.columnNames();
		final List<SingularProperty<E, ?>> uniques = new ArrayList<>(columnNames.length);
		for (final String columnName : columnNames) {
			final SingularProperty<E, ?> property = findPropertyByColumnName(columnName);
			if (property == null) {
				return;
			}
			final UniquePropertyQuality quality = UniquePropertyQuality.getMatchingQuality(property);
			if (quality != null) {
				if (quality.ordinal() > currentQuality.ordinal()) {
					currentQuality = quality;
				}
				uniques.add(property);
			}
		}
		this.allUniqueProperties.add(uniques);
		if (uniques.size() <= this.context.getMaxUniqueProperties() && isBetterUniquePropertyQuality(currentQuality)) {
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
			final GeneratedIdProperty<E, ?> generatedIdProperty = (GeneratedIdProperty<E, ?>) this.idProperty;
			// If the property "seems" to be new - according to a "0" in a primitive ID - we have to check the state,
			// as the first written ID could be "0" as well
			if (!generatedIdProperty.isNew(entity)) {
				return false;
			} else if (!generatedIdProperty.isPrimitive()) {
				return true;
			}
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
			((GeneratedIdProperty<E, ?>) this.idProperty).markReference(entity);
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
		final PendingState pendingState;
		if (state instanceof PendingState) {
			pendingState = (PendingState) state;
		} else {
			pendingState = new PendingState();
			this.entityStates.put(id, pendingState);
		}
		pendingState.addPendingUpdate(entityToUpdate, propertyToUpdate, arguments);
	}

	/**
	 * Creates a new entity of the represented {@link #getEntityClass() class}.
	 *
	 * @return the new entity
	 */
	public E newInstance() {
		try {
			return this.entityConstructor.newInstance();
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * Registers a listener that is called as soon as all properties of this class are built.
	 *
	 * If the properties are already build, the listener is called immediately.
	 *
	 * @param listener
	 *            called as soon as all properties are available
	 */
	public void onPropertiesAvailable(final Consumer<EntityClass<E>> listener) {
		if (this.builtListeners == null) {
			listener.accept(this);
		} else {
			this.builtListeners.add(listener);
		}
	}

	@Override
	public String toString() {
		return this.entityClass.getName();
	}
}
