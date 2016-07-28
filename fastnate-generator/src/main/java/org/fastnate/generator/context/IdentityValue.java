package org.fastnate.generator.context;

import java.util.Collections;
import java.util.List;

import javax.persistence.GenerationType;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

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

	private final String tableName;

	private final String columnName;

	@Getter
	private long currentValue;

	private boolean needsAlignment;

	@Override
	public void addNextValue(final InsertStatement statement, final String column, final Number value) {
		// Not necessary, as the database sets the value
	}

	@Override
	public List<? extends EntityStatement> alignNextValue() {
		if (this.needsAlignment && !this.context.isWriteRelativeIds()) {
			return this.context.getDialect().adjustNextIdentityValue(this.tableName, this.columnName,
					this.currentValue + 1);
		}
		this.needsAlignment = false;
		return Collections.emptyList();
	}

	@Override
	public long createNextValue() {
		this.needsAlignment = true;
		return ++this.currentValue;
	}

	@Override
	public List<EntityStatement> createPreInsertStatements() {
		return Collections.emptyList();
	}

	@Override
	public String getExpression(final String table, final String column, final Number targetId,
			final boolean whereExpression) {
		final long diff = this.currentValue - targetId.longValue();
		return "(SELECT max(" + column + ")" + (diff == 0 ? "" : " - " + diff) + " FROM " + table + ")";
	}

	@Override
	public boolean isPostIncrement() {
		return true;
	}

}
