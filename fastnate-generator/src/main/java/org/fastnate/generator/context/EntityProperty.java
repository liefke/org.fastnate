package org.fastnate.generator.context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Describes a property of an {@link EntityClass} that references exactly one other entity.
 *
 * @param <E>
 *            The type of the container entity
 * @param <T>
 *            The type of the target entity
 *
 * @author Tobias Liefke
 */
@Getter
public class EntityProperty<E, T> extends SingularProperty<E, T> {

	/**
	 * Helper for evaluating correct mapping information from the annotations.
	 */
	@Getter
	@RequiredArgsConstructor
	protected static class MappingInformation {

		private final Class<?> valueClass;

		private final boolean optional;

		private final String mappedBy;

		private final boolean composition;

		MappingInformation(final AttributeAccessor attribute) {
			final OneToOne oneToOne = attribute.getAnnotation(OneToOne.class);
			final NotNull notNull = attribute.getAnnotation(NotNull.class);
			if (oneToOne != null) {
				this.valueClass = oneToOne.targetEntity() == void.class ? attribute.getType() : oneToOne.targetEntity();
				this.optional = oneToOne.optional() && notNull == null;
				this.mappedBy = oneToOne.mappedBy();
				this.composition = Property.isComposition(oneToOne.cascade());
			} else {
				this.mappedBy = null;
				final ManyToOne manyToOne = attribute.getAnnotation(ManyToOne.class);
				ModelException.mustExist(manyToOne, "{} declares none of OneToOne or ManyToOne", attribute);
				this.valueClass = manyToOne.targetEntity() == void.class ? attribute.getType()
						: manyToOne.targetEntity();
				this.optional = manyToOne.optional() && notNull == null;
				this.composition = Property.isComposition(manyToOne.cascade());
			}
		}

	}

	/**
	 * Indicates that the given attribute references a single entity and may be used by an {@link EntityProperty}.
	 *
	 * @param attribute
	 *            accessor of the attribute to check
	 * @return {@code true} if an {@link EntityProperty} may be created for the given attribute
	 */
	public static boolean isEntityProperty(final AttributeAccessor attribute) {
		return attribute.isAnnotationPresent(OneToOne.class) || attribute.isAnnotationPresent(ManyToOne.class);
	}

	private static GeneratorColumn resolveJoinColumn(final GeneratorTable containerTable,
			final AttributeAccessor attribute, final AssociationOverride override, final EntityClass<?> targetClass) {
		final JoinColumn joinColumn = override != null && override.joinColumns().length > 0 ? override.joinColumns()[0]
				: attribute.getAnnotation(JoinColumn.class);
		if (joinColumn != null && joinColumn.name().length() > 0) {
			return containerTable.resolveColumn(joinColumn.name());
		}
		return containerTable.resolveColumn(attribute.getName() + '_'
				+ (targetClass == null ? "id" : targetClass.getIdColumn(attribute).getUnquotedName()));
	}

	/** The current context. */
	private final GeneratorContext context;

	/** The description of the type of this property. */
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

	/** The description of the join column. */
	private final GeneratorColumn column;

	/** The name of the id for referencing an embedded id. */
	private final String idField;

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
	 *            optional {@link AssociationOverride} configuration
	 */
	public EntityProperty(final GeneratorContext context, final GeneratorTable containerTable,
			final AttributeAccessor attribute, @Nullable final AssociationOverride override) {
		super(attribute);
		this.context = context;

		// Initialize according to the *ToOne annotations
		final MappingInformation mapping = findEntityMappingInformation(attribute);
		final Class<T> type = (Class<T>) mapping.getValueClass();
		this.targetClass = context.getDescription(type);
		this.required = !mapping.isOptional();
		this.composition = mapping.isComposition();
		this.mappedBy = StringUtils.defaultIfEmpty(mapping.getMappedBy(), null);

		final MapsId mapsId = attribute.getAnnotation(MapsId.class);
		this.idField = mapsId != null ? mapsId.value() : null;

		// Initialize the column name
		if (this.mappedBy == null) {
			this.column = resolveJoinColumn(containerTable, attribute, override, this.targetClass);
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
	}

	@Override
	public void addInsertExpression(final TableStatement statement, final E entity) {
		// Check that we are not just a "mappedBy"
		if (this.column != null) {
			// Resolve the entity
			final T value = getValue(entity);
			if (value != null) {
				final EntityClass<T> entityClass = this.context.getDescription(value);
				if (!entityClass.isNew(value)) {
					final ColumnExpression expression = entityClass.getEntityReference(value, this.idField, false);
					if (expression != null) {
						// We have an ID - use the expression
						writeColumnExpression(statement, value, expression);
						return;
					}
				}
				// If the id of the target entity is not set up to now, then the given entity is written _before_ the
				// target entity is created and we will update the property for the entity later
				entityClass.markPendingUpdates(value, entity, this);
			}
			failIfRequired(entity);
			if (this.context.isWriteNullValues()) {
				writeColumnExpression(statement, value, PrimitiveColumnExpression.NULL);
			}
		}
	}

	/**
	 * Builds the mapping information for a reference to an entity.
	 *
	 * @param attribute
	 *            accessor to the represented attribute
	 * @return the mapping information
	 */
	protected MappingInformation findEntityMappingInformation(final AttributeAccessor attribute) {
		return new MappingInformation(attribute);
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
		writeColumnExpression(stmt, (T) writtenEntity, expression);
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
		return this.column.getQualifiedName() + " = " + reference.toSql();
	}

	@Override
	public boolean isTableColumn() {
		return this.mappedBy == null;
	}

	/**
	 * Writes the column expression to the statement.
	 *
	 * @param statement
	 *            the current statement
	 * @param value
	 *            the written value
	 * @param expression
	 *            the expression of the value
	 */
	protected void writeColumnExpression(final TableStatement statement, final T value,
			final ColumnExpression expression) {
		statement.setColumnValue(this.column, expression);
	}

}
