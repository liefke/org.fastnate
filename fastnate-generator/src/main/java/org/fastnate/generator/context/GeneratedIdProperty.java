package org.fastnate.generator.context;

import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import org.apache.commons.lang.StringUtils;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;
import org.fastnate.generator.statements.UpdateStatement;

import lombok.Getter;

/**
 * Describes an {@link Id} property of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container class
 */
@Getter
public class GeneratedIdProperty<E> extends PrimitiveProperty<E, Number> {

	private static final long UNKOWN_ID_MARKER = -1L;

	private final GeneratedIds generatedIds;

	private final SequenceGenerator sequenceGenerator;

	private final String generatorId;

	private final TableGenerator tableGenerator;

	private final String generatorTable;

	private final String generatorPkColumn;

	private final String generatorPkValue;

	private final String generatorValueColumn;

	/**
	 * Creates a new instance of {@link GeneratedIdProperty}.
	 *
	 * @param entityClass
	 *            the entity class
	 * @param attribute
	 *            the accessor of the id attribute
	 * @param column
	 *            the column annotation
	 */
	public GeneratedIdProperty(final EntityClass<E> entityClass, final AttributeAccessor attribute,
			final Column column) {
		super(entityClass.getContext(), entityClass.getTable(), attribute, column);
		this.generatedIds = entityClass.getContext().getGeneratedIds();

		final GeneratedValue generation = attribute.getAnnotation(GeneratedValue.class);
		GenerationType strategy = generation.strategy();
		if (strategy == GenerationType.AUTO) {
			strategy = entityClass.getContext().getDialect().getAutoGenerationType();
		}
		if (strategy == GenerationType.TABLE) {
			this.tableGenerator = this.generatedIds.getTableGenerators().get(generation.generator());
			ModelException.test(this.tableGenerator != null, "Missing table generator: " + generation.generator());
			this.generatorTable = StringUtils.defaultIfEmpty(this.tableGenerator.table(),
					getContext().getProvider().getDefaultGeneratorTable());
			this.generatorValueColumn = StringUtils.defaultIfEmpty(this.tableGenerator.valueColumnName(),
					getContext().getProvider().getDefaultGeneratorTableValueColumnName());
			this.generatorPkColumn = StringUtils.defaultIfEmpty(this.tableGenerator.pkColumnName(),
					getContext().getProvider().getDefaultGeneratorTablePkColumnName());
			this.generatorPkValue = getContext().getDialect()
					.quoteString(StringUtils.defaultIfEmpty(this.tableGenerator.pkColumnValue(),
							getContext().getProvider().getDefaultGeneratorTablePkColumnValue()));
			this.generatorId = this.generatorTable + '.' + this.generatorPkColumn + '.' + this.generatorPkValue;
			this.sequenceGenerator = null;
		} else {
			this.tableGenerator = null;
			this.generatorTable = null;
			this.generatorPkColumn = null;
			this.generatorPkValue = null;
			this.generatorValueColumn = null;
			if (strategy == GenerationType.SEQUENCE) {
				this.sequenceGenerator = this.generatedIds.getSequenceGenerators().get(generation.generator());
				ModelException.test(this.sequenceGenerator != null,
						"Missing sequence generator: " + generation.generator());
				this.generatorId = StringUtils.defaultIfEmpty(this.sequenceGenerator.sequenceName(),
						getContext().getProvider().getDefaultSequence());
			} else { // strategy == GenerationType.IDENTITY
				this.sequenceGenerator = null;
				this.generatorId = getTable() + '.' + getColumn();
			}
		}
	}

	@Override
	public void addInsertExpression(final E entity, final InsertStatement statement) {
		final GeneratorContext context = getContext();
		if (this.tableGenerator != null) {
			final Number id = getValue(entity);
			if (id == null) {
				throw new IllegalStateException("Missing call to createPreInsertStatements");
			}
			statement.addValue(getColumn(), String.valueOf(id));
		} else {
			if (!isNew(entity)) {
				throw new IllegalArgumentException("Tried to create entity twice: " + entity);
			}
			if (context.isExplicitIds()) {
				// If we have generated explict IDs, lets do that now
				final Long id;
				if (this.sequenceGenerator != null) {
					// GenerationType.SEQUENCE
					id = this.generatedIds.createNextValue(this.sequenceGenerator);
				} else {
					// GenerationType.IDENTITY
					id = this.generatedIds.createNextValue(this.generatorId);
				}

				setValue(entity, id);
				statement.addValue(getColumn(), String.valueOf(id));
			} else if (this.sequenceGenerator != null) {
				// If we have a sequence, we can increment that one now (else we will do it in postInsert)
				setValue(entity, this.generatedIds.createNextValue(this.sequenceGenerator));
				statement.addValue(getColumn(), context.getDialect().buildNextSequenceValue(this.generatorId));
			}
		}
	}

	@Override
	public List<EntityStatement> createPreInsertStatements(final E entity) {
		if (this.tableGenerator != null) {
			if (!isNew(entity)) {
				throw new IllegalArgumentException("Tried to create entity twice: " + entity);
			}
			final Long nextValue = this.generatedIds.createNextValue(this.generatorId);
			final EntityStatement statement;
			if (nextValue == 0) {
				statement = new InsertStatement(this.generatorTable);
				statement.addValue(this.generatorPkColumn, this.generatorPkValue);
			} else {
				statement = new UpdateStatement(this.generatorTable, this.generatorPkColumn, this.generatorPkValue);
			}
			statement.addValue(this.generatorValueColumn, String.valueOf(nextValue));
			setValue(entity, nextValue);
			return Collections.singletonList(statement);
		}
		return Collections.emptyList();
	}

	/**
	 * Resolves the current value for this column.
	 *
	 * @return the current value or {@code null} if no row was created up to now
	 */
	private Long getCurrentValue() {
		if (this.sequenceGenerator != null) {
			return this.generatedIds.getCurrentValue(this.sequenceGenerator);
		}
		return this.generatedIds.getCurrentValue(this.generatorId);
	}

	/**
	 * Creates the reference of an entity in SQL using its (relative or absolute) id.
	 *
	 * @param entity
	 *            the entity
	 * @param whereExpression
	 *            indicates that the reference is used in a "where" statement
	 * @return the expression for the ID of that entity or {@code null} if the entity was not written up to now
	 * @throws IllegalArgumentException
	 *             if the entity is a {@link #isReference(Object) reference} without any id
	 */
	@Override
	public String getExpression(final E entity, final boolean whereExpression) {
		final Number targetId = getValue(entity);
		if (targetId == null) {
			return null;
		}
		if (targetId.longValue() == UNKOWN_ID_MARKER) {
			throw new IllegalArgumentException("Entity must be referenced by an unique property: " + entity);
		}
		if (targetId.longValue() < 0) {
			return String.valueOf(UNKOWN_ID_MARKER - 1 - targetId.longValue());
		}

		final GeneratorContext context = getContext();
		if (context.isExplicitIds() || this.tableGenerator != null) {
			return targetId.toString();
		}

		final long diff = getCurrentValue() - targetId.longValue();

		if (this.sequenceGenerator != null && (!whereExpression || context.getDialect().isSequenceInWhereSupported())) {
			final String reference = context.getDialect().buildCurrentSequenceValue(this.generatorId);
			if (diff == 0) {
				return reference;
			}
			return "(" + reference + " - " + diff + ")";
		}

		return "(SELECT max(" + getColumn() + ")" + (diff == 0 ? "" : " - " + diff) + " FROM " + getTable() + ")";
	}

	/**
	 * Indicates that the given entity needs to be written.
	 *
	 * @param entity
	 *            the entity to check
	 * @return {@code true} if the id of the given entity is not set up to now
	 */
	public boolean isNew(final E entity) {
		return getValue(entity) == null;
	}

	/**
	 * Indicates that the given entity was not written before, but exists already in the database.
	 *
	 * @param entity
	 *            the entity to check
	 * @return {@code true} if the entity was {@link #markReference(Object) marked as reference}
	 */
	public boolean isReference(final E entity) {
		final Number id = getValue(entity);
		return id != null && id.longValue() < 0;
	}

	/**
	 * Marks an entity as reference, where we don't know the ID database.
	 *
	 * A reference is not written during SQL generation, because it exists already in the database when the script is
	 * executed. As we don't know the ID during build time we have to reference it using some unique properties.
	 *
	 * @param entity
	 *            the entity to mark
	 */
	public void markReference(final E entity) {
		if (isNew(entity)) {
			setValue(entity, -1L);
		}
	}

	/**
	 * Marks an entity as reference, where we know the id in the database.
	 *
	 * A reference is not written during SQL generation, because it exists already in the database when the script is
	 * executed.
	 *
	 * @param entity
	 *            the entity to mark
	 * @param id
	 *            the id of the entity in the database
	 */
	public void markReference(final E entity, final long id) {
		if (isNew(entity)) {
			setValue(entity, UNKOWN_ID_MARKER - 1 - id);
		}
	}

	/**
	 * Called after the insert statement was written, to update any nessecary state in the context.
	 *
	 * @param entity
	 *            the current entity
	 */
	public void postInsert(final E entity) {
		final GeneratorContext context = getContext();
		if (!context.isExplicitIds() && this.sequenceGenerator == null && this.tableGenerator == null) {
			// We have an identity column -> the database increments the ID after the insert
			setValue(entity, this.generatedIds.createNextValue(this.generatorId));
		}
	}

}
