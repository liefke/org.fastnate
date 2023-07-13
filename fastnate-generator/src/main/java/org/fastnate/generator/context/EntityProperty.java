package org.fastnate.generator.context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import jakarta.annotation.Nullable;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes a property of an {@link EntityClass} that references another entity.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container entity
 * @param <T>
 *            The type of the target entity
 */
@Getter
public class EntityProperty<E, T> extends SingularProperty<E, T> {

	/**
	 * Helper for evaluating correct mapping information from the annotations.
	 */
	@Getter
	private static class MappingInformation {

		private final AttributeAccessor attribute;

		private final Class<?> valueClass;

		private final boolean optional;

		private final String mappedBy;

		private final Column anyColumn;

		private final String anyDefName;

		private final boolean composition;

		MappingInformation(final AttributeAccessor attribute) {
			this.attribute = attribute;
			final OneToOne oneToOne = attribute.getAnnotation(OneToOne.class);
			final NotNull notNull = attribute.getAnnotation(NotNull.class);
			if (oneToOne != null) {
				this.valueClass = oneToOne.targetEntity() == void.class ? attribute.getType() : oneToOne.targetEntity();
				this.optional = oneToOne.optional() && notNull == null;
				this.mappedBy = oneToOne.mappedBy();
				this.anyColumn = null;
				this.anyDefName = null;
				this.composition = Property.isComposition(oneToOne.cascade());
			} else {
				this.mappedBy = "";
				final ManyToOne manyToOne = attribute.getAnnotation(ManyToOne.class);
				if (manyToOne != null) {
					this.valueClass = manyToOne.targetEntity() == void.class ? attribute.getType()
							: manyToOne.targetEntity();
					this.optional = manyToOne.optional() && notNull == null;
					this.anyColumn = null;
					this.anyDefName = null;
					this.composition = Property.isComposition(manyToOne.cascade());
				} else {
					final Any any = attribute.getAnnotation(Any.class);
					ModelException.mustExist(any, "{} declares none of OneToOne, ManyToOne, or Any", attribute);
					this.valueClass = attribute.getType();
					this.optional = any.optional() && notNull == null;
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
	 * Indicates that the given attribute references an entity and may be used by an {@link EntityProperty}.
	 *
	 * @param attribute
	 *            accessor of the attribute to check
	 * @return {@code true} if an {@link EntityProperty} may be created for the given attribute
	 */
	static boolean isEntityProperty(final AttributeAccessor attribute) {
		return attribute.isAnnotationPresent(OneToOne.class) || attribute.isAnnotationPresent(ManyToOne.class)
				|| attribute.isAnnotationPresent(Any.class);
	}

	/** The current context. */
	private final GeneratorContext context;

	/**
	 * The description of the type of this property. Usually {@code null} for attributes defined as type {@link Any}.
	 */
	private final EntityClass<T> targetClass;

	/** Indicates, that this property needs a value. */
	private final boolean required;

	/**
	 * Indicates that, according to the {@link CascadeType}, we should remove the target entity when the current entity
	 * is removed.
	 */
	private final boolean composition;

	/**
	 * The optional name of a property in the target type, that owns the relationship.
	 *
	 * @see OneToOne#mappedBy()
	 */
	private final String mappedBy;

	/** The opposite property of a bidirectional mapping. */
	@Setter(AccessLevel.PACKAGE)
	private Property<T, ?> inverseProperty;

	/** The name of the join column. */
	private final GeneratorColumn column;

	/** The name of the id for referencing an embedded id. */
	private final String idField;

	/** Contains information about an addition class column, if {@link Any} is used. */
	private final AnyMapping<T> anyMapping;

	/**
	 * Creates a new instance of {@link EntityProperty}.
	 *
	 * @param context
	 *            the generator context.
	 * @param containerTable
	 *            the table that contains our column
	 * @param attribute
	 *            the accessor of the attribute
	 * @param override
	 *            optional {@link AttributeOverride} configuration.
	 */
	public EntityProperty(final GeneratorContext context, final GeneratorTable containerTable,
			final AttributeAccessor attribute, @Nullable final AssociationOverride override) {
		super(attribute);
		this.context = context;

		// Initialize according to the *ToOne annotations
		final MappingInformation mapping = new MappingInformation(attribute);
		final Class<T> type = (Class<T>) mapping.getValueClass();
		this.targetClass = context.getDescription(type);
		this.required = !mapping.isOptional();
		this.composition = mapping.isComposition();
		this.mappedBy = mapping.getMappedBy().length() == 0 ? null : mapping.getMappedBy();
		final MapsId mapsId = attribute.getAnnotation(MapsId.class);
		this.idField = mapsId != null ? mapsId.value() : null;

		// Initialize the column name
		if (this.mappedBy == null) {
			final JoinColumn joinColumn = override != null && override.joinColumns().length > 0
					? override.joinColumns()[0]
					: attribute.getAnnotation(JoinColumn.class);
			if (joinColumn != null && joinColumn.name().length() > 0) {
				this.column = containerTable.resolveColumn(joinColumn.name());
			} else {
				this.column = containerTable.resolveColumn(attribute.getName() + '_' + (this.targetClass == null ? "id"
						: this.targetClass.getIdColumn(attribute).getUnquotedName()));
			}
		} else {
			this.column = null;

			// Initialize inverseProperty as soon as the target class has all properties parsed
			this.targetClass.onPropertiesAvailable(entityClass -> {
				final Property<? super T, ?> mappedByProperty = entityClass.getProperties().get(this.mappedBy);
				if (mappedByProperty instanceof EntityProperty) {
					final EntityProperty<T, E> entityProperty = (EntityProperty<T, E>) mappedByProperty;
					this.inverseProperty = entityProperty;
					entityProperty.inverseProperty = this;
				} else {
					throw new ModelException("Unsupported \"mapped by\" property for " + getAttribute());
				}
			});
		}

		// Initialize ANY meta information
		this.anyMapping = mapping.buildAnyMapping(context, containerTable);
	}

	@Override
	public void addInsertExpression(final TableStatement statement, final E entity) {
		// Check that we are not just a "mappedBy"
		if (this.column != null) {
			// Resolve the entity
			final T value = getValue(entity);
			if (value != null) {
				if (this.anyMapping != null) {
					this.anyMapping.setColumnValue(statement, value);
				}

				final EntityClass<T> entityClass = this.context.getDescription(value);
				if (!entityClass.isNew(value)) {
					final ColumnExpression expression = entityClass.getEntityReference(value, this.idField, false);
					if (expression != null) {
						// We have an ID - use the expression
						statement.setColumnValue(this.column, expression);
						return;
					}
				}
				// If the id of the target entity is not set up to now, then the given entity is written _before_ the
				// target entity is created and we will update the property for the entity later
				entityClass.markPendingUpdates(value, entity, this);
			}
			failIfRequired(entity);
			if (this.context.isWriteNullValues()) {
				statement.setColumnValue(this.column, PrimitiveColumnExpression.NULL);
				if (this.anyMapping != null && value == null) {
					this.anyMapping.setColumnValue(statement, value);
				}
			}
		}
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		final T value = getValue(entity);
		return value == null ? Collections.emptySet() : Collections.singleton(value);
	}

	@Override
	public void generatePendingStatements(final StatementsWriter writer, final E entity, final Object writtenEntity,
			final Object... arguments) throws IOException {
		final ColumnExpression expression = this.context.getDescription(writtenEntity).getEntityReference(writtenEntity,
				this.idField, false);
		ModelException.mustExist(expression, "Entity can't be referenced: {}", writtenEntity);

		final EntityClass<E> entityClass = this.context.getDescription(entity);
		final TableStatement stmt = writer.createUpdateStatement(this.context.getDialect(), entityClass.getTable(),
				entityClass.getIdColumn(getAttribute()), entityClass.getEntityReference(entity, this.idField, true));
		stmt.setColumnValue(this.column, expression);
		writer.writeStatement(stmt);
	}

	@Override
	public ColumnExpression getExpression(final E entity, final boolean whereExpression) {
		final T value = getValue(entity);
		if (value == null) {
			return PrimitiveColumnExpression.NULL;
		}
		return EntityConverter.getEntityReference(value, this.idField, this.context, whereExpression);
	}

	@Override
	public String getPredicate(final E entity) {
		final T value = getValue(entity);
		if (value == null) {
			return this.column.getQualifiedName() + " IS NULL";
		}
		final ColumnExpression reference = EntityConverter.getEntityReference(value, this.idField, this.context, true);
		if (reference == null) {
			return null;
		}
		final String predicate = this.column.getQualifiedName() + " = " + reference.toSql();
		if (this.anyMapping == null) {
			return predicate;
		}

		return '(' + predicate + " AND " + this.anyMapping.getPredicate(value) + ')';
	}

	@Override
	public boolean isTableColumn() {
		return this.mappedBy == null;
	}

}
