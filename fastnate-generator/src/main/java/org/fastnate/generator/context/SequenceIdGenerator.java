package org.fastnate.generator.context;

import java.io.IOException;

import jakarta.persistence.SequenceGenerator;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.CurrentSequenceValueExpression;
import org.fastnate.generator.statements.NextSequenceValueExpression;
import org.fastnate.generator.statements.PlainColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Stores the current value for a {@link SequenceGenerator}.
 *
 * @author Tobias Liefke
 */
@Getter
public class SequenceIdGenerator extends IdGenerator {

	/** The current database dialect. */
	private final GeneratorDialect dialect;

	/** The (optional) catalog that contains the schema of the sequence. */
	private final String catalog;

	/** The (optional) schema that contains the sequence. */
	private final String schema;

	/** The name of the sequence. */
	private final String sequenceName;

	/** The fully qualfied name of the sequence, including the optional catalog and schema name. */
	private final String qualifiedName;

	/** Indicates that the sequence is used when referencing existing IDs, instead of absolute IDs. */
	private final boolean relativeIds;

	/** The amount to increment by when allocating sequence numbers from the sequence. */
	private final int allocationSize;

	/** The value from which the sequence object is to start generating. */
	private long initialValue;

	@Getter(AccessLevel.NONE)
	private long nextValue;

	@Getter(AccessLevel.NONE)
	private long currentSequenceValue;

	/**
	 * Creates a new instance of {@link SequenceIdGenerator}.
	 *
	 * @param generator
	 *            the annotation that contains our settings
	 * @param context
	 *            the current context
	 */
	public SequenceIdGenerator(final SequenceGenerator generator, final GeneratorContext context) {
		this.dialect = context.getDialect();
		this.catalog = StringUtils.defaultIfEmpty(generator.catalog(), null);
		this.schema = StringUtils.defaultIfEmpty(generator.schema(), null);
		this.sequenceName = generator.sequenceName();
		this.qualifiedName = context.buildQualifiedName(this.catalog, this.schema, this.sequenceName);
		this.relativeIds = context.isWriteRelativeIds();
		this.allocationSize = generator.allocationSize();
		this.nextValue = this.initialValue = generator.initialValue();
		this.currentSequenceValue = this.initialValue - 1;
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
			expression = new NextSequenceValueExpression(this, this.currentSequenceValue - value.longValue());
		} else {
			expression = new CurrentSequenceValueExpression(this, this.currentSequenceValue - value.longValue(), false);
		}
		statement.setColumnValue(column, expression);
	}

	@Override
	public void alignNextValue(final StatementsWriter writer) throws IOException {
		if (!this.relativeIds && this.nextValue > this.initialValue) {
			if (this.currentSequenceValue >= this.nextValue || this.currentSequenceValue < this.initialValue) {
				final long currentValue = this.currentSequenceValue;
				this.currentSequenceValue = this.nextValue - 1;
				this.dialect.adjustNextSequenceValue(writer, this.qualifiedName, currentValue,
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
					this.dialect.buildNextSequenceValue(this.qualifiedName, this.allocationSize));
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
			return new CurrentSequenceValueExpression(this, this.currentSequenceValue - targetId.longValue(),
					this.initialValue == this.nextValue);
		}

		final long diff = this.nextValue - 1 - targetId.longValue();
		return new PlainColumnExpression("(SELECT max(" + column.getQualifiedName() + ')'
				+ (diff == 0 ? "" : " - " + diff) + " FROM " + entityTable.getQualifiedName() + ')');
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
