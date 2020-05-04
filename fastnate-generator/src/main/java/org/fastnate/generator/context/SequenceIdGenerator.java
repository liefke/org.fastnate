package org.fastnate.generator.context;

import java.io.IOException;

import javax.persistence.SequenceGenerator;

import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.CurrentSequenceValueExpression;
import org.fastnate.generator.statements.NextSequenceValueExpression;
import org.fastnate.generator.statements.PlainColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

import lombok.Getter;

/**
 * Stores the current value for a {@link SequenceGenerator}.
 *
 * @author Tobias Liefke
 */
public class SequenceIdGenerator extends IdGenerator {

	private final GeneratorDialect dialect;

	@Getter
	private final String sequenceName;

	private final boolean relativeIds;

	@Getter
	private final int allocationSize;

	@Getter
	private long initialValue;

	private long nextValue;

	private long currentSequenceValue;

	/**
	 * Creates a new instance of {@link SequenceIdGenerator}.
	 *
	 * @param generator
	 *            the annotation that contains our settings
	 * @param dialect
	 *            the current database dialect
	 * @param relativeIds
	 *            indicates that the sequence is always used, instead of absolute IDs
	 */
	public SequenceIdGenerator(final SequenceGenerator generator, final GeneratorDialect dialect,
			final boolean relativeIds) {
		this.dialect = dialect;
		this.relativeIds = relativeIds;
		this.allocationSize = generator.allocationSize();
		this.nextValue = this.initialValue = generator.initialValue();
		this.currentSequenceValue = this.initialValue - 1;
		this.sequenceName = generator.sequenceName();
	}

	@Override
	public void addNextValue(final TableStatement statement, final GeneratorColumn column, final Number value) {
		final ColumnExpression expression;
		if (this.dialect.isNextSequenceValueInInsertSupported() && this.currentSequenceValue <= value.longValue()) {
			if (this.currentSequenceValue < this.initialValue) {
				this.currentSequenceValue = this.initialValue;
			} else {
				this.currentSequenceValue += this.allocationSize;
			}
			expression = new NextSequenceValueExpression(this.dialect, this.sequenceName, this.allocationSize,
					this.currentSequenceValue - value.longValue());
		} else {
			expression = new CurrentSequenceValueExpression(this.dialect, this.sequenceName, this.allocationSize,
					this.currentSequenceValue - value.longValue());
		}
		statement.setColumnValue(column, expression);
	}

	@Override
	public void alignNextValue(final StatementsWriter writer) throws IOException {
		if (!this.relativeIds && this.nextValue > this.initialValue) {
			if (this.currentSequenceValue >= this.nextValue || this.currentSequenceValue < this.initialValue) {
				final long currentValue = this.currentSequenceValue;
				this.currentSequenceValue = this.nextValue - 1;
				this.dialect.adjustNextSequenceValue(writer, this.sequenceName, currentValue,
						this.currentSequenceValue + this.allocationSize, this.allocationSize);
			}
		}
	}

	@Override
	public long createNextValue() {
		return this.nextValue++;
	}

	@Override
	public void createPreInsertStatements(final StatementsWriter writer) throws IOException {
		if (!this.dialect.isNextSequenceValueInInsertSupported() && this.currentSequenceValue <= this.nextValue) {
			if (this.currentSequenceValue < this.initialValue) {
				this.currentSequenceValue = this.initialValue;
			} else {
				this.currentSequenceValue += this.allocationSize;
			}
			writer.writePlainStatement(this.dialect,
					this.dialect.buildNextSequenceValue(this.sequenceName, this.allocationSize));
		}
	}

	@Override
	public long getCurrentValue() {
		return this.nextValue - 1;
	}

	@Override
	public ColumnExpression getExpression(final GeneratorTable entityTable, final GeneratorColumn column,
			final Number targetId, final boolean whereExpression) {
		if (!whereExpression || this.dialect.isSequenceInWhereSupported()) {
			return new CurrentSequenceValueExpression(this.dialect, this.sequenceName, this.allocationSize,
					this.currentSequenceValue - targetId.longValue());
		}

		final long diff = this.nextValue - 1 - targetId.longValue();
		return new PlainColumnExpression("(SELECT max(" + column.getName() + ')' + (diff == 0 ? "" : " - " + diff)
				+ " FROM " + entityTable.getQualifiedName() + ')');
	}

	@Override
	public boolean isPostIncrement() {
		return false;
	}

	@Override
	public void setCurrentValue(final long currentValue) {
		this.nextValue = this.initialValue = currentValue + 1;
		this.currentSequenceValue = currentValue;
	}

}
