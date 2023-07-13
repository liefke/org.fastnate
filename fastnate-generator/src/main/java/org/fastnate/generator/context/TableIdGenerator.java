package org.fastnate.generator.context;

import java.io.IOException;

import jakarta.persistence.TableGenerator;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.provider.JpaProvider;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PlainColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

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

	private final GeneratorTable generatorTable;

	private final GeneratorColumn pkColumn;

	private final ColumnExpression pkColumnValue;

	private final GeneratorColumn valueColumn;

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
	 * @param context
	 *            the current context
	 */
	public TableIdGenerator(final TableGenerator generator, final GeneratorContext context) {
		this.dialect = context.getDialect();
		this.relativeIds = context.isWriteRelativeIds();
		this.allocationSize = generator.allocationSize();
		ModelException.test(this.allocationSize > 0, "Only allocation sizes greater 0 are allowed, found {}",
				this.allocationSize);
		this.nextValue = this.initialValue = generator.initialValue();
		this.maxAllocatedValue = this.initialValue - 1;
		final JpaProvider provider = context.getProvider();
		this.generatorTable = context.resolveTable(generator.catalog(), generator.schema(),
				StringUtils.defaultIfEmpty(generator.table(), provider.getDefaultGeneratorTable()));
		this.valueColumn = this.generatorTable.resolveColumn(StringUtils.defaultIfEmpty(generator.valueColumnName(),
				provider.getDefaultGeneratorTableValueColumnName()));
		this.pkColumn = this.generatorTable.resolveColumn(
				StringUtils.defaultIfEmpty(generator.pkColumnName(), provider.getDefaultGeneratorTablePkColumnName()));
		final String value = StringUtils.defaultIfEmpty(generator.pkColumnValue(),
				provider.getDefaultGeneratorTablePkColumnValue());
		this.pkColumnValue = StringUtils.isEmpty(value) ? null : PrimitiveColumnExpression.create(value, this.dialect);
	}

	@Override
	public void addNextValue(final TableStatement statement, final GeneratorColumn column, final Number value) {
		statement.setColumnValue(column,
				new PlainColumnExpression(
						"(SELECT " + this.valueColumn + " - " + (getValueColumnValue() - value.longValue()) + " FROM "
								+ this.generatorTable + " WHERE " + this.pkColumn + " = " + this.pkColumnValue + ')'));
	}

	@Override
	public void alignNextValue(final StatementsWriter writer) throws IOException {
		if (this.relativeIds) {
			if (this.maxAllocatedValue >= this.nextValue) {
				final TableStatement statement = writer.createUpdateStatement(this.dialect, this.generatorTable,
						this.pkColumn, this.pkColumnValue);
				statement.setColumnValue(this.valueColumn, new PlainColumnExpression(
						this.valueColumn + " - " + (this.maxAllocatedValue - (this.nextValue - 1))));
				this.maxAllocatedValue = this.nextValue - 1;
				writer.writeStatement(statement);
			}
		} else if (this.maxAllocatedValue < this.initialValue) {
			if (this.nextValue > this.initialValue) {
				this.maxAllocatedValue = this.nextValue;
				final TableStatement statement = writer.createInsertStatement(this.dialect, this.generatorTable);
				statement.setColumnValue(this.pkColumn, this.pkColumnValue);
				statement.setColumnValue(this.valueColumn, PrimitiveColumnExpression
						.create(this.maxAllocatedValue + this.allocationSize - 1, this.dialect));
				writer.writeStatement(statement);
			}
		} else if (this.maxAllocatedValue >= this.nextValue) {
			final TableStatement statement = writer.createUpdateStatement(this.dialect, this.generatorTable,
					this.pkColumn, this.pkColumnValue);
			this.maxAllocatedValue = this.nextValue;
			statement.setColumnValue(this.valueColumn,
					PrimitiveColumnExpression.create(this.maxAllocatedValue + this.allocationSize - 1, this.dialect));
			writer.writeStatement(statement);
		}
	}

	@Override
	public long createNextValue() {
		return this.nextValue++;
	}

	@Override
	public void createPreInsertStatements(final StatementsWriter writer) throws IOException {
		if (this.maxAllocatedValue < this.nextValue) {
			final boolean firstUpdate = this.maxAllocatedValue < this.initialValue;
			this.maxAllocatedValue += this.allocationSize;

			if (this.relativeIds) {
				if (firstUpdate) {
					// Initialize the table if necessary with the predecessor of the initialValue
					writer.writePlainStatement(this.dialect, "INSERT INTO " + this.generatorTable + " (" + this.pkColumn
							+ ", " + this.valueColumn + ") SELECT " + this.pkColumnValue + ", " + this.maxAllocatedValue
							+ ' ' + this.dialect.getOptionalTable() + " WHERE NOT EXISTS (SELECT * FROM "
							+ this.generatorTable + " WHERE " + this.pkColumn + " = " + this.pkColumnValue + ')');
				}
				final TableStatement statement = writer.createUpdateStatement(this.dialect, this.generatorTable,
						this.pkColumn, this.pkColumnValue);
				statement.setColumnValue(this.valueColumn,
						new PlainColumnExpression(this.valueColumn + " + " + this.allocationSize));
				writer.writeStatement(statement);
			} else {
				final TableStatement statement;
				if (firstUpdate) {
					statement = writer.createInsertStatement(this.dialect, this.generatorTable);
					statement.setColumnValue(this.pkColumn, this.pkColumnValue);
				} else {
					statement = writer.createUpdateStatement(this.dialect, this.generatorTable, this.pkColumn,
							this.pkColumnValue);
				}
				statement.setColumnValue(this.valueColumn,
						PrimitiveColumnExpression.create(this.maxAllocatedValue + this.allocationSize, this.dialect));
				writer.writeStatement(statement);
			}
		}
	}

	@Override
	public IdGenerator derive(final GeneratorTable currentTable) {
		if (this.pkColumnValue == null) {
			return new TableIdGenerator(this.dialect, this.relativeIds, this.generatorTable, this.pkColumn,
					PrimitiveColumnExpression.create(currentTable.getUnquotedName(), this.dialect), this.valueColumn,
					this.allocationSize, this.initialValue, this.nextValue, this.maxAllocatedValue);
		}
		return this;
	}

	@Override
	public long getCurrentValue() {
		return this.nextValue - 1;
	}

	@Override
	public ColumnExpression getExpression(final GeneratorTable table, final GeneratorColumn column,
			final Number targetId, final boolean whereExpression) {
		final long diff = getValueColumnValue() - targetId.longValue();
		return new PlainColumnExpression("(SELECT " + this.valueColumn.getQualifiedName()
				+ (diff == 0 ? "" : " - " + diff) + " FROM " + this.generatorTable.getQualifiedName() + " WHERE "
				+ this.pkColumn.getQualifiedName() + " = " + this.pkColumnValue + ')');
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
