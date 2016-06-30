package org.fastnate.generator.statements;

import lombok.RequiredArgsConstructor;

/**
 * A single (unparsed) SQL statement.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class PlainStatement extends EntityStatement {

	private final String sql;

	@Override
	public String toSql() {
		return this.sql + ";\n";
	}

}
