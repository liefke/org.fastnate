package org.fastnate.generator.context;

import java.io.IOException;

import jakarta.persistence.GenerationType;

import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PlainColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Contains the current value for a primary key of type {@link GenerationType#IDENTITY}.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class IdentityValue extends IdGenerator {

	private final GeneratorContext context;

	private final GeneratorTable table;

	private final GeneratorColumn column;

	@Getter
	private long currentValue;

	private boolean needsAlignment;

	@Override
	public void addNextValue(final TableStatement statement, final GeneratorColumn tableColumn, final Number value) {
		// Not necessary, as the database sets the value
	}

	@Override
	public void alignNextValue(final StatementsWriter writer) throws IOException {
		if (this.needsAlignment) {
			this.needsAlignment = false;
			if (!this.context.isWriteRelativeIds() && this.context.getDialect().isSettingIdentityAllowed()) {
				this.context.getDialect().adjustNextIdentityValue(writer, this.table, this.column,
						this.currentValue + 1);
			}
		}
	}

	@Override
	public long createNextValue() {
		this.needsAlignment = true;
		return ++this.currentValue;
	}

	@Override
	public ColumnExpression getExpression(final GeneratorTable entityTable, final GeneratorColumn targetColumn,
			final Number targetId, final boolean whereExpression) {
		final long diff = this.currentValue - targetId.longValue();
		return new PlainColumnExpression("(SELECT max(" + this.column.getQualifiedName() + ")"
				+ (diff == 0 ? "" : " - " + diff) + " FROM " + this.table.getQualifiedName() + ")");
	}

	@Override
	public boolean isPostIncrement() {
		return true;
	}

	@Override
	public void setCurrentValue(final long currentValue) {
		this.needsAlignment = false;
		this.currentValue = currentValue;
	}

}
