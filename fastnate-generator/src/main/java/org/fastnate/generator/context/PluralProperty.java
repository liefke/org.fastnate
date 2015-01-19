package org.fastnate.generator.context;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapsId;

import lombok.Getter;

import org.fastnate.generator.statements.InsertStatement;

/**
 * Base class for {@link MapProperty} and {@link CollectionProperty}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container class
 * @param <C>
 *            The type of the collection of map
 * @param <T>
 *            The type of the elements in the collection
 */
@Getter
public abstract class PluralProperty<E, C, T> extends Property<E, C> {

	/**
	 * Builds the name of the column that contains the ID of the entity for the given property.
	 *
	 * @param accessor
	 *            the accessor for the inspected property
	 * @param override
	 *            contains optional override options
	 * @param collectionMetadata
	 *            the default join column
	 * @param defaultIdColumn
	 *            the default name for the column, if {@code joinColumn} is empty or {@code null}
	 * @return the column name
	 */
	protected static String buildIdColumn(final PropertyAccessor accessor, final AssociationOverride override,
			final CollectionTable collectionMetadata, final String defaultIdColumn) {
		return buildIdColumn(accessor, override, collectionMetadata != null ? collectionMetadata.joinColumns() : null,
				defaultIdColumn);
	}

	/**
	 * Builds the name of the column that contains the ID of the entity for the given field.
	 *
	 * @param field
	 *            the inspected field
	 * @param override
	 *            contains optional override options
	 * @param joinColumns
	 *            the default join columns
	 * @param defaultIdColumn
	 *            the default name for the column, if {@code joinColumn} is empty or {@code null}
	 * @return the column name
	 */
	private static String buildIdColumn(final PropertyAccessor field, final AssociationOverride override,
			final JoinColumn[] joinColumns, final String defaultIdColumn) {
		if (override != null && override.joinColumns().length > 0) {
			final JoinColumn joinColumn = override.joinColumns()[0];
			if (joinColumn.name().length() > 0) {
				return joinColumn.name();
			}
		}

		final JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		if (joinColumn != null && joinColumn.name().length() > 0) {
			return joinColumn.name();
		}

		if (joinColumns != null && joinColumns.length > 0 && joinColumns[0].name().length() > 0) {
			return joinColumns[0].name();
		}
		return defaultIdColumn;
	}

	/**
	 * Builds the name of the column that contains the ID of the entity for the given field.
	 *
	 * @param field
	 *            the inspected field
	 * @param override
	 *            contains optional override options
	 * @param joinTable
	 *            the optional join table date
	 * @param tableMetadata
	 *            the optional
	 * @param defaultIdColumn
	 *            the default name for the column, if {@code joinColumn} is empty or {@code null}
	 * @return the column name
	 */
	protected static String buildIdColumn(final PropertyAccessor field, final AssociationOverride override,
			final JoinTable joinTable, final CollectionTable tableMetadata, final String defaultIdColumn) {
		return buildIdColumn(field, override, joinTable != null ? joinTable.joinColumns()
				: tableMetadata != null ? tableMetadata.joinColumns() : null, defaultIdColumn);
	}

	/**
	 * Builds the name of the table of the association for the given field.
	 *
	 * @param tableMetadata
	 *            the current metadata of the field
	 * @param defaultTableName
	 *            the default name for the table
	 * @return the column name
	 */
	protected static String buildTableName(final CollectionTable tableMetadata, final String defaultTableName) {
		return tableMetadata != null && tableMetadata.name().length() != 0 ? tableMetadata.name() : defaultTableName;
	}

	/**
	 * Builds the name of the table of the association for the given field.
	 *
	 * @param attribute
	 *            the inspected field
	 * @param override
	 *            contains optional override options
	 * @param joinTable
	 *            the optional join table
	 * @param collectionTable
	 *            the optional metadata of the table
	 * @param defaultTableName
	 *            the default name for the table
	 * @return the table name
	 */
	protected static String buildTableName(final PropertyAccessor attribute, final AssociationOverride override,
			final JoinTable joinTable, final CollectionTable collectionTable, final String defaultTableName) {
		if (override != null) {
			final JoinTable joinTableOverride = override.joinTable();
			if (joinTableOverride != null && joinTableOverride.name().length() > 0) {
				return joinTableOverride.name();
			}
		}
		if (joinTable != null && joinTable.name().length() > 0) {
			return joinTable.name();
		}
		if (collectionTable != null && collectionTable.name().length() > 0) {
			return collectionTable.name();
		}
		return defaultTableName;
	}

	/**
	 * Builds the name of the column that contains the value for the collection / map.
	 *
	 * @param field
	 *            the inspected field
	 * @param defaultValueColumn
	 *            the default name
	 * @return the column name
	 */
	protected static String buildValueColumn(final PropertyAccessor field, final String defaultValueColumn) {
		final JoinTable tableMetadata = field.getAnnotation(JoinTable.class);
		if (tableMetadata != null && tableMetadata.inverseJoinColumns().length > 0
				&& tableMetadata.inverseJoinColumns()[0].name().length() > 0) {
			return tableMetadata.inverseJoinColumns()[0].name();
		}
		final Column columnMetadata = field.getAnnotation(Column.class);
		if (columnMetadata != null && columnMetadata.name().length() > 0) {
			return columnMetadata.name();
		}
		return defaultValueColumn;
	}

	/**
	 * Inspects the given field and searches for a generic type argument.
	 *
	 * @param field
	 *            the field to inspect
	 * @param explicitClass
	 *            an explicit class to use, if the metadata specified one
	 * @param argumentIndex
	 *            the index of the argument, for maps there are two: the key and the value
	 * @return the found class
	 */
	@SuppressWarnings("unchecked")
	protected static <T> Class<T> getPropertyArgument(final PropertyAccessor field, final Class<T> explicitClass,
			final int argumentIndex) {
		if (explicitClass != void.class) {
			// Explict target class
			return explicitClass;
		}

		// Inspect the type binding
		if (!(field.getGenericType() instanceof ParameterizedType)) {
			throw new IllegalArgumentException(field + " is not of generic type and has no defined entity class");
		}

		final ParameterizedType type = (ParameterizedType) field.getGenericType();
		final Type[] parameterArgTypes = type.getActualTypeArguments();
		if (parameterArgTypes.length > argumentIndex) {
			Type genericType = parameterArgTypes[argumentIndex];
			if (genericType instanceof ParameterizedType) {
				genericType = ((ParameterizedType) genericType).getRawType();
			}
			if (genericType instanceof Class<?>) {
				return (Class<T>) genericType;
			}
		}
		throw new IllegalArgumentException(field + " has illegal generic type signature");

	}

	/** The current context. */
	private final GeneratorContext context;

	/** Contains all properties of an embedded element collection. */
	private List<SingularProperty<T, ?>> embeddedProperties;

	/** The property to use, if an id is embedded. */
	private final String mappedId;

	/**
	 * Creates a new property.
	 *
	 * @param context
	 *            the current context
	 * @param field
	 *            the field
	 */
	public PluralProperty(final GeneratorContext context, final PropertyAccessor field) {
		super(field);
		this.context = context;

		final MapsId mapsId = field.getAnnotation(MapsId.class);
		this.mappedId = mapsId == null || mapsId.value().length() == 0 ? null : mapsId.value();
	}

	@Override
	public void addInsertExpression(final E entity, final InsertStatement statement) {
		// Ignore
	}

	/**
	 * Builds the embedded properties of this property.
	 *
	 * @param targetType
	 *            the target type
	 */
	protected void buildEmbeddedProperties(final Class<?> targetType) {
		if (targetType.isAnnotationPresent(Embeddable.class)) {
			// Determine the access style
			AccessStyle accessStyle;
			final Access accessType = targetType.getAnnotation(Access.class);
			if (accessType != null) {
				accessStyle = AccessStyle.getStyle(accessType.value());
			} else {
				accessStyle = getAccessor().getAccessStyle();
			}

			this.embeddedProperties = new ArrayList<>();
			final Map<String, AttributeOverride> attributeOverrides = EntityClass.getAttributeOverrides(getAccessor());
			for (final PropertyAccessor field : accessStyle.getDeclaredProperties(targetType)) {
				final AttributeOverride attributeOveride = attributeOverrides.get(field.getName());
				final Column columnMetadata = attributeOveride != null ? attributeOveride.column() : field
						.getAnnotation(Column.class);
				final AssociationOverride assocOverride = EntityClass.getAccociationOverrides(getAccessor()).get(
						field.getName());
				final SingularProperty<T, ?> property = buildProperty(field, columnMetadata, assocOverride);
				if (property != null) {
					this.embeddedProperties.add(property);
				}
			}
		}
	}

	private SingularProperty<T, ?> buildProperty(final PropertyAccessor accessor, final Column columnMetadata,
			final AssociationOverride override) {
		// Ignore static, transient and generated fields
		if (accessor.isPersistentProperty()) {
			if (CollectionProperty.isCollectionProperty(accessor) || MapProperty.isMapProperty(accessor)) {
				throw new IllegalArgumentException("Plural attributes not allowed for embedded element collection: "
						+ accessor);
			}
			if (EntityProperty.isEntityProperty(accessor)) {
				return new EntityProperty<>(this.context, accessor, override);
			}
			return new PrimitiveProperty<>(this.context, getTable(), accessor, columnMetadata);
		}
		return null;
	}

	/**
	 * The name of the column that contains the ID of the entity that contains this collection.
	 *
	 * @return the name of the ID column
	 */
	public abstract String getIdColumn();

	/**
	 * The name of the table of this property, if any.
	 *
	 * @return the name of the table
	 */
	public abstract String getTable();

	/**
	 * The name of the column that contains the values of the collection.
	 *
	 * @return the name of the value column or {@code null} if {@link #isEmbedded() embedded}
	 */
	public abstract String getValueColumn();

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

}