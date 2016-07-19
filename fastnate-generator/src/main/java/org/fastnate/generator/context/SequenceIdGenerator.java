package org.fastnate.generator.context;

import java.util.Collections;
import java.util.List;

import javax.persistence.SequenceGenerator;

import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;
import org.fastnate.generator.statements.PlainStatement;

/**
 * Stores the current value for a {@link SequenceGenerator}.
 *
 * @author Tobias Liefke
 */
public class SequenceIdGenerator extends IdGenerator {

	private final int initialValue;

	private final int allocationSize;

	private final GeneratorDialect dialect;

	private final String sequenceName;

	private long nextValue;

	private long currentSequenceValue;

	/**
	 * Creates a new instance of {@link SequenceIdGenerator}.
	 *
	 * @param generator
	 *            the annotation that contains our settings
	 * @param dialect
	 *            the current database dialect
	 */
	public SequenceIdGenerator(final SequenceGenerator generator, final GeneratorDialect dialect) {
		this.dialect = dialect;
		this.allocationSize = generator.allocationSize();
		this.nextValue = this.initialValue = generator.initialValue();
		this.currentSequenceValue = this.initialValue - 1;
		this.sequenceName = generator.sequenceName();
	}

	@Override
	public void addNextValue(final InsertStatement statement, final String column, final Number value) {
		String expression;
		if (this.dialect.isNextSequenceValueInInsertSupported() && this.currentSequenceValue <= value.longValue()) {
			if (this.currentSequenceValue < this.initialValue) {
				this.currentSequenceValue = this.initialValue;
			} else {
				this.currentSequenceValue += this.allocationSize;
			}
			expression = this.dialect.buildNextSequenceValue(this.sequenceName, this.allocationSize);
		} else {
			expression = this.dialect.buildCurrentSequenceValue(this.sequenceName, this.allocationSize);
		}

		final long diff = this.currentSequenceValue - value.longValue();
		if (diff != 0) {
			expression = '(' + expression + " - " + diff + ')';
		}
		statement.addValue(column, expression);
	}

	@Override
	public List<? extends EntityStatement> alignNextValue() {
		if (this.currentSequenceValue >= this.nextValue) {
			final long currentValue = this.currentSequenceValue;
			this.currentSequenceValue = this.nextValue - 1;
			return this.dialect.adjustNextSequenceValue(this.sequenceName, currentValue,
					this.currentSequenceValue + this.allocationSize, this.allocationSize);
		}
		return null;
	}

	@Override
	public long createNextValue() {
		return this.nextValue++;
	}

	@Override
	public List<? extends EntityStatement> createPreInsertStatements() {
		if (!this.dialect.isNextSequenceValueInInsertSupported() && this.currentSequenceValue <= this.nextValue) {
			if (this.currentSequenceValue < this.initialValue) {
				this.currentSequenceValue = this.initialValue;
			} else {
				this.currentSequenceValue += this.allocationSize;
			}
			return Collections.singletonList(
					new PlainStatement(this.dialect.buildNextSequenceValue(this.sequenceName, this.allocationSize)));
		}
		return Collections.emptyList();
	}

	@Override
	public long getCurrentValue() {
		return this.nextValue - 1;
	}

	@Override
	public String getExpression(final String table, final String column, final Number targetId,
			final boolean whereExpression) {
		if (!whereExpression || this.dialect.isSequenceInWhereSupported()) {
			final String reference = this.dialect.buildCurrentSequenceValue(this.sequenceName, this.allocationSize);
			final long diff = this.currentSequenceValue - targetId.longValue();
			if (diff == 0) {
				return reference;
			}
			return "(" + reference + " - " + diff + ")";
		}

		final long diff = this.nextValue - 1 - targetId.longValue();
		return "(SELECT max(" + column + ")" + (diff == 0 ? "" : " - " + diff) + " FROM " + table + ")";
	}

	@Override
	public boolean isPostIncrement() {
		return false;
	}

}
