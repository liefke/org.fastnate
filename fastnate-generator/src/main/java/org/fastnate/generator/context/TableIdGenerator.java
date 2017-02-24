package org.fastnate.generator.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.TableGenerator;

import org.apache.commons.lang.StringUtils;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.provider.JpaProvider;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;
import org.fastnate.generator.statements.PlainStatement;
import org.fastnate.generator.statements.TableStatement;
import org.fastnate.generator.statements.UpdateStatement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Saves the current value for a {@link TableGenerator}.
 *
 * The content of the value column is interpreted like <i>nextValue</i> of a sequence, which means the maximum allocated
 * value is allways at most {@code value column value - allocationSize}.
 *
 * @author Tobias Liefke
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableIdGenerator extends IdGenerator {

	private final GeneratorDialect dialect;

	private final boolean relativeIds;

	private final String generatorTable;

	private final String pkColumnName;

	private final String pkColumnValue;

	private final String valueColumnName;

	private final int allocationSize;

	private long initialValue;

	private long nextValue;

	/**
	 * The maximum allocated value.
	 *
	 * The value column value in the table is at any time by {@link #allocationSize} greater.
	 */
	private long maxAllocatedValue;

	/**
	 * Creates a new instance of {@link SequenceIdGenerator}.
	 *
	 * @param generator
	 *            the annotation that contains our settings
	 * @param dialect
	 *            the dialect of the database
	 * @param provider
	 *            the provider of the target JPA framework
	 * @param relativeIds
	 *            indicates that the database is <b>not</b> empty
	 */
	public TableIdGenerator(final TableGenerator generator, final GeneratorDialect dialect, final JpaProvider provider,
			final boolean relativeIds) {
		this.dialect = dialect;
		this.relativeIds = relativeIds;
		this.allocationSize = generator.allocationSize();
		ModelException.test(this.allocationSize > 0, "Only allocation sizes greater 0 are allowed, found {}",
				this.allocationSize);
		this.nextValue = this.initialValue = generator.initialValue();
		this.maxAllocatedValue = this.initialValue - 1;
		this.generatorTable = StringUtils.defaultIfEmpty(generator.table(), provider.getDefaultGeneratorTable());
		this.valueColumnName = StringUtils.defaultIfEmpty(generator.valueColumnName(),
				provider.getDefaultGeneratorTableValueColumnName());
		this.pkColumnName = StringUtils.defaultIfEmpty(generator.pkColumnName(),
				provider.getDefaultGeneratorTablePkColumnName());
		final String value = StringUtils.defaultIfEmpty(generator.pkColumnValue(),
				provider.getDefaultGeneratorTablePkColumnValue());
		this.pkColumnValue = StringUtils.isEmpty(value) ? null : dialect.quoteString(value);
	}

	@Override
	public void addNextValue(final InsertStatement statement, final String column, final Number value) {
		statement.addValue(column,
				"(SELECT " + this.valueColumnName + " - " + (getValueColumnValue() - value.longValue()) + " FROM "
						+ this.generatorTable + " WHERE " + this.pkColumnName + " = " + this.pkColumnValue + ')');
	}

	@Override
	public List<? extends EntityStatement> alignNextValue() {
		if (this.relativeIds) {
			if (this.maxAllocatedValue >= this.nextValue) {
				final UpdateStatement statement = new UpdateStatement(this.generatorTable, this.pkColumnName,
						this.pkColumnValue);
				statement.addValue(this.valueColumnName,
						this.valueColumnName + " - " + (this.maxAllocatedValue - (this.nextValue - 1)));
				this.maxAllocatedValue = this.nextValue - 1;
				return Collections.singletonList(statement);
			}
		} else if (this.maxAllocatedValue < this.initialValue) {
			if (this.nextValue > this.initialValue) {
				this.maxAllocatedValue = this.nextValue;
				final InsertStatement statement = new InsertStatement(this.generatorTable);
				statement.addValue(this.pkColumnName, this.pkColumnValue);
				statement.addValue(this.valueColumnName,
						String.valueOf(this.maxAllocatedValue + this.allocationSize - 1));
				return Collections.singletonList(statement);
			}
		} else if (this.maxAllocatedValue >= this.nextValue) {
			final UpdateStatement statement = new UpdateStatement(this.generatorTable, this.pkColumnName,
					this.pkColumnValue);
			this.maxAllocatedValue = this.nextValue;
			statement.addValue(this.valueColumnName, String.valueOf(this.maxAllocatedValue + this.allocationSize - 1));
			return Collections.singletonList(statement);
		}
		return Collections.emptyList();
	}

	@Override
	public long createNextValue() {
		return this.nextValue++;
	}

	@Override
	public List<? extends EntityStatement> createPreInsertStatements() {
		if (this.maxAllocatedValue >= this.nextValue) {
			return Collections.emptyList();
		}

		final boolean firstUpdate = this.maxAllocatedValue < this.initialValue;
		this.maxAllocatedValue += this.allocationSize;

		if (this.relativeIds) {
			final List<EntityStatement> result = new ArrayList<>(2);
			if (firstUpdate) {
				// Initialize the table if necessary with the predecessor of the initialValue
				result.add(new PlainStatement("INSERT INTO " + this.generatorTable + " (" + this.pkColumnName + ", "
						+ this.valueColumnName + ") SELECT " + this.pkColumnValue + ", " + this.maxAllocatedValue + ' '
						+ this.dialect.getOptionalTable() + " WHERE NOT EXISTS (SELECT * FROM " + this.generatorTable
						+ " WHERE " + this.pkColumnName + " = " + this.pkColumnValue + ')'));
			}
			final UpdateStatement statement = new UpdateStatement(this.generatorTable, this.pkColumnName,
					this.pkColumnValue);
			statement.addValue(this.valueColumnName, this.valueColumnName + " + " + this.allocationSize);
			result.add(statement);
			return result;
		}
		final TableStatement statement;
		if (firstUpdate) {
			statement = new InsertStatement(this.generatorTable);
			statement.addValue(this.pkColumnName, this.pkColumnValue);
		} else {
			statement = new UpdateStatement(this.generatorTable, this.pkColumnName, this.pkColumnValue);
		}
		statement.addValue(this.valueColumnName, String.valueOf(this.maxAllocatedValue + this.allocationSize));
		return Collections.singletonList(statement);
	}

	@Override
	public IdGenerator derive(final String currentTable) {
		if (StringUtils.isEmpty(this.pkColumnValue)) {
			return new TableIdGenerator(this.dialect, this.relativeIds, this.generatorTable, this.pkColumnName,
					this.dialect.quoteString(currentTable), this.valueColumnName, this.allocationSize,
					this.initialValue, this.nextValue, this.maxAllocatedValue);
		}
		return this;
	}

	@Override
	public long getCurrentValue() {
		return this.nextValue - 1;
	}

	@Override
	public String getExpression(final String tableName, final String columnName, final Number targetId,
			final boolean whereExpression) {
		final long diff = getValueColumnValue() - targetId.longValue();
		return "(SELECT " + this.valueColumnName + (diff == 0 ? "" : " - " + diff) + " FROM " + this.generatorTable
				+ " WHERE " + this.pkColumnName + " = " + this.pkColumnValue + ')';
	}

	private long getValueColumnValue() {
		return this.maxAllocatedValue + this.allocationSize;
	}

	@Override
	public boolean isPostIncrement() {
		return false;
	}

	@Override
	public void setCurrentValue(final long currentValue) {
		this.nextValue = currentValue + 1;
		this.maxAllocatedValue = currentValue;
	}

}
