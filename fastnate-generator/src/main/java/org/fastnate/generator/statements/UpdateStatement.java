package org.fastnate.generator.statements;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds the information for an SQL update statement.
 * 
 * @author Tobias Liefke
 */
@Getter
@Setter
public class UpdateStatement extends EntityStatement {

	private final String idColumn;

	private final String idValue;

	/**
	 * Creates a new instance of UpdateStatement.
	 * 
	 * @param table
	 *            the affected table
	 * @param idColumn
	 *            the column that contains the id of the changed entity
	 * @param idValue
	 *            the id of the changed entity
	 */
	public UpdateStatement(final String table, final String idColumn, final String idValue) {
		super(table);
		this.idColumn = idColumn;
		this.idValue = idValue;
	}

	/**
	 * Creates the SQL for this statement.
	 * 
	 * @return the resulting SQL
	 */
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder("UPDATE ").append(getTable()).append(" SET ");
		for (final Iterator<Map.Entry<String, String>> entries = getValues().entrySet().iterator(); entries.hasNext();) {
			final Entry<String, String> entry = entries.next();
			result.append(entry.getKey()).append(" = ").append(entry.getValue());
			if (entries.hasNext()) {
				result.append(", ");
			}
		}
		result.append(" WHERE ").append(this.idColumn).append(" = ").append(this.idValue).append(";\n");
		return result.toString();
	}
}
