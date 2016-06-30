package org.fastnate.generator.statements;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A SQL statement that affects a table and a set of columns.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public abstract class TableStatement extends EntityStatement {

	/** The main table of this update / insert statement. */
	private final String table;

	/** The columns and their values. */
	private final Map<String, String> values = new LinkedHashMap<>();

	/**
	 * Adds a value to the list of value expressions.
	 *
	 * @param column
	 *            the column name
	 * @param value
	 *            the expression value (as expression, that is including surrounding ' for string literals)
	 */
	public void addValue(final String column, final String value) {
		if (value == null) {
			throw new NullPointerException("value should be not null");
		}
		this.values.put(column, value);
	}

}
