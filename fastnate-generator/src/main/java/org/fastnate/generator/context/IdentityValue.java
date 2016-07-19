package org.fastnate.generator.context;

import java.util.Collections;
import java.util.List;

import javax.persistence.GenerationType;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

import lombok.Getter;

/**
 * Contains the current value for a primary key of type {@link GenerationType#IDENTITY}.
 *
 * @author Tobias Liefke
 */
public class IdentityValue extends IdGenerator {

	@Getter
	private long currentValue;

	@Override
	public void addNextValue(final InsertStatement statement, final String columnName, final Number value) {
		// Not necessary, as the database sets the value
	}

	@Override
	public List<? extends EntityStatement> alignNextValue() {
		// We are already aligned
		return Collections.emptyList();
	}

	@Override
	public long createNextValue() {
		return ++this.currentValue;
	}

	@Override
	public List<EntityStatement> createPreInsertStatements() {
		return Collections.emptyList();
	}

	@Override
	public String getExpression(final String tableName, final String columnName, final Number targetId,
			final boolean whereExpression) {
		final long diff = this.currentValue - targetId.longValue();
		return "(SELECT max(" + columnName + ")" + (diff == 0 ? "" : " - " + diff) + " FROM " + tableName + ")";
	}

	@Override
	public boolean isPostIncrement() {
		return true;
	}

}
