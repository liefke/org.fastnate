package org.fastnate.generator.context;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stores the metadata about existing database tables.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public class GeneratorTable {

	/** The index of this table in the list of all available tables. */
	private final int index;

	/** The name of this table. */
	private final String name;

	/** The current generator context. */
	private final GeneratorContext context;

	/** The known columns for this table. */
	private final Map<String, GeneratorColumn> columns = new LinkedHashMap<>();

	/**
	 * Adds or finds a column which is part of an insert statement to this statement.
	 *
	 * @param columnName
	 *            the name of the new column
	 * @return the found resp. created column
	 */
	public GeneratorColumn resolveColumn(final String columnName) {
		return resolveColumn(columnName, false);
	}

	/**
	 * Adds or finds a column which is part of this table.
	 *
	 * @param columnName
	 *            the name of the new column
	 * @param autoGenerated
	 *            {@code true} if this column is not part of any insert statement, because it is generated by the
	 *            database
	 * @return the found resp. created column
	 */
	public GeneratorColumn resolveColumn(final String columnName, final boolean autoGenerated) {
		GeneratorColumn column = this.columns.get(columnName);
		if (column == null) {
			column = new GeneratorColumn(this, this.columns.size(), columnName, autoGenerated);
			this.columns.put(columnName, column);
			this.context.fireContextObjectAdded(ContextModelListener::foundColumn, column);
		}
		return column;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
