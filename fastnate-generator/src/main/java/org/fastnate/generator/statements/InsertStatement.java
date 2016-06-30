package org.fastnate.generator.statements;

import com.google.common.base.Joiner;

import lombok.Getter;

/**
 * Holds the information for an SQL Insert statement.
 *
 * @author Tobias Liefke
 */
@Getter
public class InsertStatement extends TableStatement {

	private static final Joiner JOINER = Joiner.on(", ");

	/**
	 * Creates a new instance of InsertStatement.
	 *
	 * @param table
	 *            the affected table
	 */
	public InsertStatement(final String table) {
		super(table);
	}

	@Override
	public String toSql() {
		final StringBuilder result = new StringBuilder("INSERT INTO ").append(getTable());
		if (getValues().isEmpty()) {
			// Can happen if we have an generated identity column and only null values
			result.append(" DEFAULT VALUES;\n");
		} else {
			JOINER.appendTo(result.append(' ').append('('), getValues().keySet()).append(") VALUES (");
			JOINER.appendTo(result, getValues().values()).append(");\n");
		}
		return result.toString();
	}
}
