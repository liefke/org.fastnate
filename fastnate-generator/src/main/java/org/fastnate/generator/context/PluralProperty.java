package org.fastnate.generator.context;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.persistence.Access;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;
import org.fastnate.util.ClassUtil;
import org.hibernate.annotations.ManyToAny;

import lombok.Getter;

/**
 * Base class for {@link MapProperty} and {@link CollectionProperty}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container class
 * @param <C>
 *            The type of the collection or map
 * @param <T>
 *            The type of the elements in the collection
 */
@Getter
public abstract class PluralProperty<E, C, T> extends Property<E, C> {

	/**
	 * Helper for evaluating correct mapping information from the annotations.
	 */
	@Getter
	private static class MappingInformation {

		private static boolean useTargetTable(final AttributeAccessor attribute, final AssociationOverride override) {
			final JoinColumn joinColumn = override != null && override.joinColumns().length > 0
					? override.joinColumns()[0]
					: attribute.getAnnotation(JoinColumn.class);
			final JoinTable joinTable = override != null && override.joinTable() != null ? override.joinTable()
					: attribute.getAnnotation(JoinTable.class);
			return joinColumn != null && joinTable == null;
		}

		private final AttributeAccessor attribute;

		private final Class<?> valueClass;

		private final String mappedBy;

		private final boolean useTargetTable;

		private final Column anyColumn;

		private final String anyDefName;

		private final boolean composition;

		MappingInformation(final AttributeAccessor attribute, final AssociationOverride override,
				final int valueArgumentIndex) {
			this.attribute = attribute;
			final OneToMany oneToMany = attribute.getAnnotation(OneToMany.class);
			if (oneToMany != null) {
				this.valueClass = getPropertyArgument(attribute, oneToMany.targetEntity(), valueArgumentIndex);
				this.mappedBy = oneToMany.mappedBy().length() == 0 ? null : oneToMany.mappedBy();
				this.useTargetTable = this.mappedBy != null || useTargetTable(attribute, override);
				this.anyColumn = null;
				this.anyDefName = null;
				this.composition = Property.isComposition(oneToMany.cascade());
			} else {
				final ManyToMany manyToMany = attribute.getAnnotation(ManyToMany.class);
				if (manyToMany != null) {
					this.valueClass = getPropertyArgument(attribute, manyToMany.targetEntity(), valueArgumentIndex);
					this.mappedBy = manyToMany.mappedBy().length() == 0 ? null : manyToMany.mappedBy();
					this.useTargetTable = this.mappedBy != null;
					this.anyColumn = null;
					this.anyDefName = null;
					this.composition = Property.isComposition(manyToMany.cascade());
				} else {
					final ManyToAny manyToAny = attribute.getAnnotation(ManyToAny.class);
					ModelException.mustExist(manyToAny,
							"{} declares none of OneToMany, ManyToMany, ElementCollection, or AnyToMany", attribute);
					this.valueClass = getPropertyArgument(attribute, void.class, valueArgumentIndex);
					this.mappedBy = null;
					this.useTargetTable = false;
					this.anyColumn = null;
					this.anyDefName = null;
					this.composition = false;
				}
			}
		}

		<T> AnyMapping<T> buildAnyMapping(final GeneratorContext context, final GeneratorTable containerTable) {
			if (this.anyColumn == null) {
				return null;
			}
			return new AnyMapping<>(context, this.attribute, containerTable, this.anyColumn, this.anyDefName);
		}
	}

	/**
	 * Builds the name of the column that contains the ID of the entity for the given attribute.
	 *
	 * @param attribute
	 *            the accessor for the inspected attribute
	 * @param override
	 *            contains optional override options
	 * @param collectionMetadata
	 *            the default join column
	 * @param defaultIdColumn
	 *            the default name for the column, if {@code joinColumn} is empty or {@code null}
	 * @return the column name
	 */
	protected static String buildIdColumn(final AttributeAccessor attribute, final AssociationOverride override,
			final CollectionTable collectionMetadata, final String defaultIdColumn) {
		return buildIdColumn(attribute, override, collectionMetadata != null ? collectionMetadata.joinColumns() : null,
				defaultIdColumn);
	}

	private static String buildIdColumn(final AttributeAccessor attribute, final AssociationOverride override,
			final JoinColumn[] joinColumns, final String defaultIdColumn) {
		if (override != null) {
			String joinCloumnName = getJoinColumnName(override.joinColumns());
			if (joinCloumnName != null) {
				return joinCloumnName;
			}
			joinCloumnName = getJoinColumnName(override.joinTable().joinColumns());
			if (joinCloumnName != null) {
				return joinCloumnName;
			}
		}

		final JoinColumn joinColumn = attribute.getAnnotation(JoinColumn.class);
		if (joinColumn != null && joinColumn.name().length() > 0) {
			return joinColumn.name();
		}

		return StringUtils.defaultString(getJoinColumnName(joinColumns), defaultIdColumn);
	}

	private static String buildIdColumn(final AttributeAccessor attribute, final AssociationOverride override,
			final JoinTable joinTable, final CollectionTable tableMetadata, final String defaultIdColumn) {
		return buildIdColumn(attribute, override, joinTable != null ? joinTable.joinColumns()
				: tableMetadata != null ? tableMetadata.joinColumns() : null, defaultIdColumn);
	}

	/**
	 * Builds the column that contains the value for the collection / map.
	 *
	 * @param table
	 *            the collection table
	 * @param asscoiationOverride
	 *            contains optional override options for association
	 * @param attributeOverride
	 *            contains optional override options for element collections
	 * @param attribute
	 *            the inspected attribute
	 * @param defaultColumnName
	 *            the default column name
	 * @return the column definition
	 */
	private static GeneratorColumn buildValueColumn(final GeneratorTable table,
			final AssociationOverride asscoiationOverride, final AttributeOverride attributeOverride,
			final AttributeAccessor attribute, final String defaultColumnName) {
		if (asscoiationOverride != null) {
			final String joinColumnName = getJoinColumnName(asscoiationOverride.joinTable().inverseJoinColumns());
			if (joinColumnName != null) {
				return table.resolveColumn(joinColumnName);
			}
		}

		if (attributeOverride != null) {
			final Column column = attributeOverride.column();
			if (column != null && column.name().length() > 0) {
				return table.resolveColumn(column.name());
			}
		}

		final JoinTable tableMetadata = attribute.getAnnotation(JoinTable.class);
		if (tableMetadata != null) {
			final String joinColumnName = getJoinColumnName(tableMetadata.inverseJoinColumns());
			if (joinColumnName != null) {
				return table.resolveColumn(joinColumnName);
			}
		}

		final Column columnMetadata = attribute.getAnnotation(Column.class);
		if (columnMetadata != null && columnMetadata.name().length() > 0) {
			return table.resolveColumn(columnMetadata.name());
		}
		return table.resolveColumn(defaultColumnName);
	}

	private static String findMappedId(final AttributeAccessor attribute) {
		final MapsId mapsId = attribute.getAnnotation(MapsId.class);
		return mapsId == null || mapsId.value().length() == 0 ? null : mapsId.value();
	}

	private static <T> Constructor<T> findValueConstructor(final Class<T> valueClass) {
		try {
			final Constructor<T> constructor = valueClass.getConstructor();
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor;
		} catch (final NoSuchMethodException e) {
			// Ignore and just don't support calls to newElement()
			return null;
		}
	}

	private static String getJoinColumnName(final JoinColumn[] joinColumns) {
		if (joinColumns != null && joinColumns.length > 0) {
			final JoinColumn joinColumn = joinColumns[0];
			if (joinColumn.name().length() > 0) {
				return joinColumn.name();
			}
		}
		return null;
	}

	/**
	 * Inspects the given attribute and searches for a generic type argument.
	 *
	 * @param attribute
	 *            the attribute to inspect
	 * @param explicitClass
	 *            an explicit class to use, if the metadata specified one
	 * @param argumentIndex
	 *            the index of the argument, for maps there are two: the key and the value
	 * @return the found class
	 */
	protected static <T> Class<T> getPropertyArgument(final AttributeAccessor attribute, final Class<T> explicitClass,
			final int argumentIndex) {
		if (explicitClass != void.class) {
			// Explict target class
			return explicitClass;
		}

		// Inspect the type binding
		ModelException.test(attribute.getGenericType() instanceof ParameterizedType,
				"{} is not of generic type and has no defined entity class", attribute);

		final ParameterizedType type = (ParameterizedType) attribute.getGenericType();
		final Type[] parameterArgTypes = type.getActualTypeArguments();
		ModelException.test(argumentIndex < parameterArgTypes.length, "{} has illegal generic type signature",
				attribute);
		return ClassUtil.getActualTypeBinding(attribute.getImplementationClass(),
				(Class<Object>) attribute.getDeclaringClass(), parameterArgTypes[argumentIndex]);
	}

	/**
	 * Indicates, that the given attribute has an annotation that indicates a plural property.
	 *
	 * @param attribute
	 *            the attribute to check
	 * @return {@code true} if one of the plural annotations is defined for the attribute
	 */
	protected static boolean hasPluralAnnotation(final AttributeAccessor attribute) {
		return attribute.isAnnotationPresent(OneToMany.class) || attribute.isAnnotationPresent(ManyToMany.class)
				|| attribute.isAnnotationPresent(ElementCollection.class)
				|| attribute.isAnnotationPresent(ManyToAny.class);
	}

	private static String resolveAnnotationAttribute(final AssociationOverride override, final JoinTable joinTable,
			final CollectionTable collectionTable, final Function<JoinTable, String> joinTableAttribute,
			final Function<CollectionTable, String> collectionTableAttribute, final String defaultAttribute) {
		if (override != null) {
			final JoinTable joinTableOverride = override.joinTable();
			if (joinTableOverride != null) {
				final String value = joinTableAttribute.apply(joinTableOverride);
				if (value.length() > 0) {
					return value;
				}
			}
		}
		if (joinTable != null) {
			final String value = joinTableAttribute.apply(joinTable);
			if (value.length() > 0) {
				return value;
			}
		}
		if (collectionTable != null) {
			final String value = collectionTableAttribute.apply(collectionTable);
			if (value.length() > 0) {
				return value;
			}
		}
		return defaultAttribute;
	}

	private static GeneratorTable resolveTable(final GeneratorContext context, final AssociationOverride override,
			final JoinTable joinTable, final CollectionTable collectionTable, final GeneratorTable sourceEntityTable,
			final String targetEntityTable) {
		return context.resolveTable(
				resolveAnnotationAttribute(override, joinTable, collectionTable, JoinTable::catalog,
						CollectionTable::catalog, sourceEntityTable.getCatalog()),
				resolveAnnotationAttribute(override, joinTable, collectionTable, JoinTable::schema,
						CollectionTable::schema, sourceEntityTable.getSchema()),
				resolveAnnotationAttribute(override, joinTable, collectionTable, JoinTable::name, CollectionTable::name,
						sourceEntityTable.getUnquotedName() + '_' + targetEntityTable));
	}

	/** The current context. */
	private final GeneratorContext context;

	/** The current database dialect, as defined in the context. */
	private final GeneratorDialect dialect;

	/** Contains all properties of an embedded element collection. */
	private List<Property<T, ?>> embeddedProperties;

	/** Contains all properties of an embedded element collection by their name. */
	private Map<String, Property<T, ?>> embeddedPropertiesByName;

	/** Contains all properties of an embedded element collection which reference a required entity. */
	private List<EntityProperty<T, ?>> requiredEmbeddedProperties;

	/** The property to use, if an id is embedded. */
	private final String mappedId;

	/** The modified table. */
	private final GeneratorTable table;

	/** The column that contains the id of the entity. */
	private GeneratorColumn idColumn;

	/**
	 * Indicates that, according to the {@link CascadeType}, we should remove the target entities when the current
	 * entity is removed.
	 *
	 * Always {@code true} for {@link ElementCollection}s.
	 */
	private final boolean composition;

	/** Indicates that this property is defined by another property on the target type. */
	private final String mappedBy;

	/**
	 * The opposite property of a bidirectional mapping.
	 *
	 * Can either be another {@link PluralProperty} (it the relationship is a {@link ManyToMany}) or an
	 * {@link EntityProperty} (if the relationship is a {@link OneToMany}.
	 */
	private Property<T, ?> inverseProperty;

	/** Indicates to use a column of the target table. */
	private final boolean useTargetTable;

	/** The column that contains the value (or the id of the value). */
	private final GeneratorColumn valueColumn;

	/** The class of the value of the collection. */
	private final Class<T> valueClass;

	/** The noargs constructor for the values of the collection. */
	private final Constructor<T> valueConstructor;

	/** Indicates that entities are referenced by the collection. */
	private final boolean entityReference;

	/** The description of the {@link #valueClass}, {@code null} if not an entity or {@link ManyToAny} is used. */
	private final EntityClass<T> valueEntityClass;

	/** The converter for the value of the collection, {@code null} if not a primitive value. */
	private final ValueConverter<T> valueConverter;

	/** Contains information about an addition class column, if {@link ManyToAny} is used. */
	private final AnyMapping<T> anyMapping;

	/**
	 * Creates a new property.
	 *
	 * @param sourceClass
	 *            the description of the current inspected class that contains this property
	 * @param attribute
	 *            accessor to the represented attribute
	 * @param associationOverride
	 *            the configured assocation override
	 * @param attributeOverride
	 *            the configured attribute override, if we reference an {@link ElementCollection}
	 * @param valueClassParamIndex
	 *            the index of the value argument in the collection class (0 for collection, 1 for map)
	 */
	@SuppressWarnings("checkstyle:JavaNCSS")
	public PluralProperty(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride associationOverride, final AttributeOverride attributeOverride,
			final int valueClassParamIndex) {
		super(attribute);
		this.context = sourceClass.getContext();
		this.dialect = this.context.getDialect();

		this.mappedId = findMappedId(attribute);

		// Check if we are OneToMany, ManyToMany, ManyToAny, or ElementCollection and initialize accordingly
		final CollectionTable collectionTable = attribute.getAnnotation(CollectionTable.class);
		final ElementCollection values = attribute.getAnnotation(ElementCollection.class);
		this.entityReference = values == null;
		if (values == null) {
			// Entity mapping, either OneToMany, ManyToMany, or ManyToAny
			final MappingInformation mapping = new MappingInformation(attribute, associationOverride,
					valueClassParamIndex);
			this.mappedBy = mapping.getMappedBy();
			this.useTargetTable = mapping.isUseTargetTable();
			this.composition = mapping.isComposition();

			// Resolve the target entity class
			this.valueClass = (Class<T>) mapping.getValueClass();
			this.valueEntityClass = sourceClass.getContext().getDescription(this.valueClass);

			// An entity mapping needs an entity class
			ModelException.test(this.valueEntityClass != null || mapping.getAnyColumn() != null,
					"{} has no entity as value", attribute);

			// No primitive value
			this.valueConverter = null;

			// Initialize the table and column names
			if (this.mappedBy != null) {
				// Bidirectional - use the columns of the target class
				this.table = this.valueEntityClass.getTable();
				initializeInverseProperty();
				this.valueColumn = buildValueColumn(this.table, null, null, attribute,
						this.valueEntityClass.getIdColumn(attribute).getName());
				this.anyMapping = null;
			} else if (this.useTargetTable) {
				// Unidirectional and join column is in the table of the target class
				this.table = this.valueEntityClass.getTable();
				this.idColumn = this.table.resolveColumn(buildIdColumn(attribute, associationOverride, null, null,
						attribute.getName() + '_' + sourceClass.getIdColumn(attribute).getUnquotedName()));
				this.valueColumn = buildValueColumn(this.table, null, null, attribute,
						this.valueEntityClass.getIdColumn(attribute).getName());
				this.anyMapping = null;
			} else {
				// We need a mapping table
				final JoinTable joinTable = attribute.getAnnotation(JoinTable.class);
				this.table = resolveTable(this.context, associationOverride, joinTable, collectionTable,
						sourceClass.getTable(),
						this.valueEntityClass == null ? "table" : this.valueEntityClass.getTable().getUnquotedName());
				initializeIdColumnForMappingTable(sourceClass, attribute, associationOverride, joinTable,
						collectionTable);
				this.valueColumn = buildValueColumn(this.table, associationOverride, null, attribute,
						attribute.getName() + '_' + (this.valueEntityClass == null ? "id"
								: this.valueEntityClass.getIdColumn(attribute).getUnquotedName()));
				this.anyMapping = mapping.buildAnyMapping(this.context, this.table);
			}
		} else {
			// We are the owning side of the mapping
			this.mappedBy = null;
			this.useTargetTable = false;
			this.anyMapping = null;
			this.composition = true;

			// Initialize the table and id column name
			this.table = this.context.resolveTable(associationOverride, collectionTable, CollectionTable::catalog,
					CollectionTable::schema, CollectionTable::name,
					sourceClass.getEntityName() + '_' + attribute.getName());
			this.idColumn = this.table.resolveColumn(buildIdColumn(attribute, associationOverride, collectionTable,
					sourceClass.getEntityName() + '_' + sourceClass.getIdColumn(attribute).getUnquotedName()));

			// Initialize the target description and columns
			this.valueClass = getPropertyArgument(attribute, values.targetClass(), valueClassParamIndex);
			this.valueEntityClass = null;
			if (this.valueClass.isAnnotationPresent(Embeddable.class)) {
				buildEmbeddedProperties(sourceClass, this.valueClass);
				this.valueConverter = null;
				this.valueColumn = null;
			} else {
				// Check for primitive value
				this.valueConverter = this.context.getProvider().createConverter(attribute, this.valueClass, false);
				this.valueColumn = buildValueColumn(this.table, null, attributeOverride, attribute,
						attribute.getName());
			}
		}

		this.valueConstructor = findValueConstructor(this.valueClass);
	}

	@Override
	public void addInsertExpression(final TableStatement statement, final E entity) {
		// Ignore
	}

	/**
	 * Builds the embedded properties of this property.
	 *
	 * @param sourceClass
	 *            the class that contains our property
	 *
	 * @param targetType
	 *            the target type
	 */
	protected void buildEmbeddedProperties(final EntityClass<?> sourceClass, final Class<?> targetType) {
		if (targetType.isAnnotationPresent(Embeddable.class)) {
			this.embeddedProperties = new ArrayList<>();
			this.embeddedPropertiesByName = new HashMap<>();
			this.requiredEmbeddedProperties = new ArrayList<>();

			final AccessStyle accessStyle;
			final Access accessType = targetType.getAnnotation(Access.class);
			if (accessType != null) {
				accessStyle = AccessStyle.getStyle(accessType.value());
			} else {
				accessStyle = getAttribute().getAccessStyle();
			}

			final String prefix = getAttribute().getName() + '.';
			final Map<String, AttributeOverride> attributeOverrides = EntityClass
					.getAttributeOverrides(sourceClass.getAttributeOverrides(), prefix, getAttribute().getElement());
			final Map<String, AssociationOverride> accociationOverrides = EntityClass.getAccociationOverrides(
					sourceClass.getAssociationOverrides(), prefix, getAttribute().getElement());
			for (final AttributeAccessor attribute : accessStyle.getDeclaredAttributes((Class<Object>) targetType,
					getAttribute().getImplementationClass())) {
				if (attribute.isPersistent()) {
					final Property<T, ?> property = sourceClass.buildProperty(this.table, attribute, attributeOverrides,
							accociationOverrides);

					this.embeddedProperties.add(property);
					this.embeddedPropertiesByName.put(property.getName(), property);
					if (property instanceof EntityProperty && property.isRequired()) {
						this.requiredEmbeddedProperties.add((EntityProperty<T, ?>) property);
					}
				}
			}
		}
	}

	/**
	 * Creates the statements for a (not embeddable) value from the collection.
	 *
	 * @param writer
	 *            the target of created statements
	 * @param entity
	 *            the entity that contains the collection resp. map.
	 * @param sourceId
	 *            the ID of the current entity
	 * @param key
	 *            the value of the {@link #getKeyColumn()} - either the index or the map key
	 * @param value
	 *            the current value in the collection
	 * @throws IOException
	 *             if the writer throws one
	 */
	protected void createDirectValueStatement(final StatementsWriter writer, final E entity,
			final ColumnExpression sourceId, final ColumnExpression key, final T value) throws IOException {
		final ColumnExpression target = createValueExpression(entity, sourceId, value, key);
		if (target != null) {
			final TableStatement stmt;
			if (this.useTargetTable) {
				// Unidirectional, but from target table
				if (value == null) {
					return;
				}
				stmt = writer.createUpdateStatement(this.dialect, this.table, this.valueColumn, target);
				if (this.mappedBy == null) {
					stmt.setColumnValue(this.idColumn, sourceId);
				}
			} else {
				stmt = writer.createInsertStatement(this.dialect, this.table);
				stmt.setColumnValue(this.idColumn, sourceId);
				stmt.setColumnValue(this.valueColumn, target);
				if (this.anyMapping != null) {
					this.anyMapping.setColumnValue(stmt, value);
				}
			}

			final GeneratorColumn keyColumn = getKeyColumn();
			if (keyColumn != null) {
				stmt.setColumnValue(keyColumn, key);
			}
			writer.writeStatement(stmt);
		}
	}

	/**
	 * Writes all embedded properties of a value of a collection table.
	 *
	 * @param writer
	 *            the target of the created statement
	 *
	 * @param entity
	 *            the entity that contains the collection resp. map.
	 * @param sourceId
	 *            the ID of the current entity
	 * @param key
	 *            the value of the {@link #getKeyColumn()} - either the index or the map key
	 * @param value
	 *            the current value in the collection
	 * @throws IOException
	 *             if the writer throws one
	 */
	private void createEmbeddedValueStatement(final StatementsWriter writer, final E entity,
			final ColumnExpression sourceId, final ColumnExpression key, final T value) throws IOException {
		for (final EntityProperty<T, ?> requiredProperty : this.requiredEmbeddedProperties) {
			final Object referencedEntity = requiredProperty.getValue(value);
			if (referencedEntity != null) {
				final EntityClass<Object> targetDescription = (EntityClass<Object>) this.context
						.getDescription(referencedEntity.getClass());
				final Property<Object, ?> idProperty = targetDescription.getIdProperty();
				if (idProperty.getValue(referencedEntity) == null) {
					// At least one of the required entities is not written up to now
					targetDescription.markPendingUpdates(referencedEntity, entity, this, key, value);
					return;
				}
			}
		}

		final TableStatement stmt = writer.createInsertStatement(this.dialect, getTable());
		stmt.setColumnValue(getIdColumn(), sourceId);
		if (getKeyColumn() != null) {
			stmt.setColumnValue(getKeyColumn(), key);
		}

		for (final Property<T, ?> property : this.embeddedProperties) {
			property.addInsertExpression(stmt, value);
		}
		writer.writeStatement(stmt);
	}

	/**
	 * Creates a SQL expression for a value of the collection, as long as it is not embedded.
	 *
	 * @param entity
	 *            the entity that contains the collection
	 * @param sourceId
	 *            the id of the entity as SQL expression
	 * @param key
	 *            the key/index of the entity in the collection as SQL expression
	 * @param value
	 *            the value that is written
	 * @return the expression or {@code null} if the insert is still pending
	 */
	protected ColumnExpression createValueExpression(final E entity, final ColumnExpression sourceId, final T value,
			final ColumnExpression key) {
		if (value == null) {
			return PrimitiveColumnExpression.NULL;
		}
		if (this.valueConverter != null) {
			return this.valueConverter.getExpression(value, getContext());
		}

		final EntityClass<T> entityClass = this.valueEntityClass == null ? this.context.getDescription(value)
				: this.valueEntityClass;

		final ColumnExpression target = entityClass.getEntityReference(value, this.mappedId, this.useTargetTable);
		if (target == null) {
			// Not created up to now
			entityClass.markPendingUpdates(value, entity, this, key, value);
		}
		return target;
	}

	/**
	 * Adds a value statement to the list of collection statements.
	 *
	 * @param writer
	 *            the target of the created statements
	 * @param entity
	 *            the entity that contains the collection
	 * @param sourceId
	 *            the id of the entity as SQL expression
	 * @param key
	 *            the key/index of the entity in the collection as SQL expression
	 * @param value
	 *            the value that is written
	 * @throws IOException
	 *             if the writer throws one
	 */
	protected void createValueStatement(final StatementsWriter writer, final E entity, final ColumnExpression sourceId,
			final ColumnExpression key, final T value) throws IOException {
		if (isEmbedded()) {
			createEmbeddedValueStatement(writer, entity, sourceId, key, value);
		} else {
			createDirectValueStatement(writer, entity, sourceId, key, value);
		}
	}

	@Override
	public void generatePendingStatements(final StatementsWriter writer, final E entity, final Object writtenEntity,
			final Object... arguments) throws IOException {
		final ColumnExpression sourceId = EntityConverter.getEntityReference(entity, this.mappedId, getContext(),
				false);
		createValueStatement(writer, entity, sourceId, (ColumnExpression) arguments[0], (T) arguments[1]);
	}

	/**
	 * An optional column that contains the index / key of the values.
	 *
	 * @return the key column for map properties resp. the index column for list properties
	 */
	protected abstract GeneratorColumn getKeyColumn();

	private void initializeIdColumnForMappingTable(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride override, final JoinTable joinTable, final CollectionTable collectionTable) {
		// Name of the ID column could be derived from a "mapped by" property in the target class
		final String idColumnName = buildIdColumn(attribute, override, joinTable, collectionTable, null);
		if (idColumnName != null) {
			this.idColumn = this.table.resolveColumn(idColumnName);
		} else {
			this.valueEntityClass.onPropertiesAvailable(entityClass -> {
				final String entityName = entityClass.getAllProperties().stream().filter(p -> {
					if (p instanceof PluralProperty) {
						final PluralProperty<?, ?, ?> other = (PluralProperty<?, ?, ?>) p;
						return other.getValueEntityClass() == sourceClass && getName().equals(other.getMappedBy());
					}
					return false;
				}).map(Property::getName).findFirst().orElseGet(sourceClass::getEntityName);
				this.idColumn = this.table
						.resolveColumn(entityName + '_' + sourceClass.getIdColumn(getAttribute()).getUnquotedName());
			});
		}
	}

	private void initializeInverseProperty() {
		this.valueEntityClass.onPropertiesAvailable(entityClass -> {
			this.inverseProperty = (Property<T, ?>) entityClass.getProperties().get(this.mappedBy);
			if (this.inverseProperty instanceof EntityProperty) {
				final EntityProperty<T, E> entityProperty = (EntityProperty<T, E>) this.inverseProperty;
				this.idColumn = entityProperty.getColumn();
				entityProperty.setInverseProperty(this);
			} else if (this.inverseProperty instanceof PluralProperty) {
				final PluralProperty<T, ?, E> pluralProperty = (PluralProperty<T, ?, E>) this.inverseProperty;
				this.idColumn = pluralProperty.getValueColumn();
				pluralProperty.inverseProperty = this;
			} else {
				throw new ModelException("Unsupported \"mapped by\" property for " + getAttribute());
			}
		});
	}

	/**
	 * Indicates that this propery is a {@link ElementCollection} that references {@link Embeddable}s.
	 *
	 * @return true if {@link #getEmbeddedProperties()} returns a list of properties
	 */
	public boolean isEmbedded() {
		return this.embeddedProperties != null;
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public boolean isTableColumn() {
		return false;
	}

	/**
	 * Tries to create a new instance of an element using the parameter-less constructor.
	 *
	 * @return the new element
	 */
	public T newElement() {
		if (this.valueConstructor == null) {
			throw new UnsupportedOperationException("The element class has no parameter-less constructor");
		}
		try {
			return this.valueConstructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new UnsupportedOperationException("Could not create new element", e);
		}
	}

}