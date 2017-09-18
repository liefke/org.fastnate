package org.fastnate.generator.context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

import lombok.Getter;

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

		private final boolean optional;

		private final String mappedBy;

		private final String anyMetaColumn;

		MappingInformation(final AttributeAccessor attribute) {
			final OneToOne oneToOne = attribute.getAnnotation(OneToOne.class);
			final NotNull notNull = attribute.getAnnotation(NotNull.class);
			if (oneToOne != null) {
				this.optional = oneToOne.optional() && notNull == null;
				this.mappedBy = oneToOne.mappedBy();
				this.anyMetaColumn = null;
			} else {
				this.mappedBy = "";
				final ManyToOne manyToOne = attribute.getAnnotation(ManyToOne.class);
				if (manyToOne != null) {
					this.optional = manyToOne.optional() && notNull == null;
					this.anyMetaColumn = null;
				} else {
					final Any any = attribute.getAnnotation(Any.class);
					if (any != null) {
						this.optional = any.optional() && notNull == null;
						this.anyMetaColumn = any.metaColumn().name();
					} else {
						final ManyToAny manyToAny = attribute.getAnnotation(ManyToAny.class);
						if (manyToAny == null) {
							throw new IllegalArgumentException(
									attribute + " is neither declared as OneToOne nor ManyToOne");
						}
						this.optional = notNull == null;
						this.anyMetaColumn = manyToAny.metaColumn().name();
					}
				}
			}
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
				|| attribute.isAnnotationPresent(Any.class) || attribute.isAnnotationPresent(ManyToAny.class);
	}

	/** The current context. */
	private final GeneratorContext context;

	/** The description of the type of this property. */
	private final EntityClass<T> targetClass;

	/** Indicates, that this property needs a value. */
	private final boolean required;

	/** Indicates if this property is defined by another property on the target type. */
	private final String mappedBy;

	/** The name of the join column. */
	private final GeneratorColumn column;

	/** The name of the id for referencing an embedded id. */
	private final String idField;

	/** The name of the column that contains the id of the entity class, if {@link Any} or {@link ManyToAny} is used. */
	private final GeneratorColumn anyColumn;

	/** Contains the mapping from a class to its id in the database. */
	private final Map<Class<?>, ColumnExpression> anyClasses = new HashMap<>();

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

		// Initialize the target class description
		this.targetClass = context.getDescription((Class<T>) attribute.getType());

		// Initialize according to the *ToOne annotations
		final MappingInformation mapping = new MappingInformation(attribute);
		this.required = !mapping.isOptional();
		this.mappedBy = mapping.getMappedBy().length() == 0 ? null : mapping.getMappedBy();
		final MapsId mapsId = attribute.getAnnotation(MapsId.class);
		this.idField = mapsId != null ? mapsId.value() : null;

		// Initialize the column name
		if (this.mappedBy == null) {
			final JoinColumn joinColumn = override != null && override.joinColumns().length > 0
					? override.joinColumns()[0] : attribute.getAnnotation(JoinColumn.class);
			if (joinColumn != null && joinColumn.name().length() > 0) {
				this.column = containerTable.resolveColumn(joinColumn.name());
			} else {
				this.column = containerTable.resolveColumn(attribute.getName() + "_"
						+ (this.targetClass == null ? "id" : this.targetClass.getIdColumn(attribute)));
			}
		} else {
			this.column = null;
		}

		// Initialize ANY meta information
		final String anyColumnName = mapping.getAnyMetaColumn();
		if (anyColumnName != null) {
			this.anyColumn = containerTable.resolveColumn(anyColumnName);
			fillMetaDefs(attribute, context.getDialect());
		} else {
			this.anyColumn = null;
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
				final ColumnExpression expression = entityClass.getEntityReference(value, this.idField, false);
				if (expression != null) {
					// We have an ID - use the expression
					statement.setColumnValue(this.column, expression);
					if (this.anyColumn != null) {
						statement.setColumnValue(this.anyColumn, findAnyDesc(value));
					}
					return;
				}
				// If the id of the target entity is not set up to now, then the given entity is written _before_ the
				// target entity is created and we will update the property for the entity later
				entityClass.markPendingUpdates(value, entity, this);
			}
			failIfRequired(entity);
			if (this.context.isWriteNullValues()) {
				statement.setColumnValue(this.column, PrimitiveColumnExpression.NULL);
				if (this.anyColumn != null) {
					statement.setColumnValue(this.anyColumn, PrimitiveColumnExpression.NULL);
				}
			}
		}
	}

	private void fillMetaDefs(final AttributeAccessor attribute, final GeneratorDialect dialect) {
		final AnyMetaDef metaDef = attribute.getAnnotation(AnyMetaDef.class);
		if (metaDef == null) {
			throw new IllegalArgumentException("Missing AnyMetaDef for " + attribute);
		}
		for (final MetaValue metaValue : metaDef.metaValues()) {
			this.anyClasses.put(metaValue.targetEntity(), PrimitiveColumnExpression.create(metaValue.value(), dialect));
		}
	}

	private ColumnExpression findAnyDesc(final T entity) {
		final ColumnExpression desc = this.anyClasses.get(entity.getClass());
		if (desc == null) {
			throw new IllegalArgumentException(
					"Can'f find meta description for " + entity.getClass() + " on " + getAttribute());
		}
		return desc;
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
		if (expression == null) {
			throw new ModelException("Entity can't be referenced: " + writtenEntity);
		}
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
			return this.column.getName() + " IS NULL";
		}
		final ColumnExpression reference = EntityConverter.getEntityReference(value, this.idField, this.context, true);
		if (reference == null) {
			return null;
		}
		if (this.anyColumn != null) {
			return '(' + this.column.getName() + " = " + reference.toSql() + " AND " + this.anyColumn.getName() + " = "
					+ findAnyDesc(value) + ')';
		}
		return this.column + " = " + reference.toSql();
	}

	@Override
	public boolean isTableColumn() {
		return this.mappedBy == null;
	}

}
