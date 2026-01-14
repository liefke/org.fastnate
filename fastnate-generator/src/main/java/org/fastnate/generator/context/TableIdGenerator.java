package org.fastnate.generator.context;

import java.io.IOException;

import jakarta.persistence.TableGenerator;

import org.apache.commons.lang3.StringUtils;
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

	private final GeneratorContext context;

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
	 * @param tableName
	 *            the name of the current table
	 * @param generator
	 *            the annotation that contains our settings
	 * @param context
	 *            the current context
	 */
	public TableIdGenerator(final String tableName, final TableGenerator generator, final GeneratorContext context) {
		this.context = context;
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
				provider.getDefaultGeneratorTablePkColumnValue(tableName));
		this.pkColumnValue = StringUtils.isEmpty(value) ? PrimitiveColumnExpression.NULL
				: PrimitiveColumnExpression.create(value, this.context.getDialect());
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
				final TableStatement statement = writer.createUpdateStatement(this.context.getDialect(),
						this.generatorTable, this.pkColumn, this.pkColumnValue);
				statement.setColumnValue(this.valueColumn, new PlainColumnExpression(
						this.valueColumn + " - " + (this.maxAllocatedValue - (this.nextValue - 1))));
				this.maxAllocatedValue = this.nextValue - 1;
				writer.writeStatement(statement);
			}
		} else if (this.maxAllocatedValue < this.initialValue) {
			if (this.nextValue > this.initialValue) {
				this.maxAllocatedValue = this.nextValue - 1;
				writeAllocatedValue(writer, true, this.maxAllocatedValue + this.allocationSize - 1);
			}
		} else if (this.maxAllocatedValue >= this.nextValue) {
			this.maxAllocatedValue = this.nextValue - 1;
			writeAllocatedValue(writer, false, this.maxAllocatedValue + this.allocationSize - 1);
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
					if (this.context.getProvider().isInitializingGeneratorTables()) {
						// Ensure that we have at least our allocation size written
						writer.writePlainStatement(this.context.getDialect(), "UPDATE " + this.generatorTable + " SET "
								+ this.valueColumn + " = " + this.maxAllocatedValue + " WHERE " + this.pkColumn + " = "
								+ this.pkColumnValue + " AND " + this.valueColumn + " < " + this.maxAllocatedValue);
					} else {
						// Initialize the table if necessary with the predecessor of the initialValue
						writer.writePlainStatement(this.context.getDialect(),
								"INSERT INTO " + this.generatorTable + " (" + this.pkColumn + ", " + this.valueColumn
										+ ") SELECT " + this.pkColumnValue + ", " + this.maxAllocatedValue + ' '
										+ this.context.getDialect().getOptionalTable()
										+ " WHERE NOT EXISTS (SELECT * FROM " + this.generatorTable + " WHERE "
										+ this.pkColumn + " = " + this.pkColumnValue + ')');
					}
				}
				final TableStatement statement = writer.createUpdateStatement(this.context.getDialect(),
						this.generatorTable, this.pkColumn, this.pkColumnValue);
				statement.setColumnValue(this.valueColumn,
						new PlainColumnExpression(this.valueColumn + " + " + this.allocationSize));
				writer.writeStatement(statement);
			} else {
				writeAllocatedValue(writer, firstUpdate, this.maxAllocatedValue + this.allocationSize);
			}
		}
	}

	@Override
	public IdGenerator derive(final GeneratorTable currentTable) {
		if (this.pkColumnValue == null) {
			return new TableIdGenerator(this.context, this.relativeIds, this.generatorTable, this.pkColumn,
					PrimitiveColumnExpression.create(currentTable.getUnquotedName(), this.context.getDialect()),
					this.valueColumn, this.allocationSize, this.initialValue, this.nextValue, this.maxAllocatedValue);
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
		return new PlainColumnExpression("(SELECT GREATEST(0, " + this.valueColumn.getQualifiedName()
				+ (diff == 0 ? "" : " - " + diff) + ") FROM " + this.generatorTable.getQualifiedName() + " WHERE "
				+ this.pkColumn.getQualifiedName() + " = " + this.pkColumnValue + ')');
	}

	private long getValueColumnValue() {
		return this.maxAllocatedValue + this.allocationSize - 1;
	}

	@Override
	public boolean isPostIncrement() {
		return false;
	}

	@Override
	public void setCurrentValue(final long currentValue) {
		if (currentValue == 0) {
			this.nextValue = 0;
			this.maxAllocatedValue = -1;
		} else {
			this.nextValue = currentValue + 1;
			this.maxAllocatedValue = currentValue;
		}
	}

	private void writeAllocatedValue(final StatementsWriter writer, final boolean firstUpdate,
			final long allocatedValue) throws IOException {
		final TableStatement statement;
		if (firstUpdate && !this.context.getProvider().isInitializingGeneratorTables()) {
			statement = writer.createInsertStatement(this.context.getDialect(), this.generatorTable);
			statement.setColumnValue(this.pkColumn, this.pkColumnValue);
		} else {
			statement = writer.createUpdateStatement(this.context.getDialect(), this.generatorTable, this.pkColumn,
					this.pkColumnValue);
		}
		statement.setColumnValue(this.valueColumn,
				PrimitiveColumnExpression.create(allocatedValue, this.context.getDialect()));
		writer.writeStatement(statement);
	}

}
