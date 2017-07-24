package org.fastnate.generator.statements;

import lombok.RequiredArgsConstructor;

/**
 * An SQL expression, which is printed to SQL "as is".
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class PlainColumnExpression implements ColumnExpression {

	private final String sql;

	@Override
	public String toSql() {
		return this.sql;
	}

	@Override
	public String toString() {
		return this.sql;
	}

}
