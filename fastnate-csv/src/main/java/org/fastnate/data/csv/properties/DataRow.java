package org.fastnate.data.csv.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A row from an import file.
 *
 * Especially useful in CSV and table calculation files.
 *
 * Don't keep a reference to this row in consumers, as it may be reused across different rows.
 *
 * @author Tobias Liefke
 */
public abstract class DataRow {

	private final Map<String, Integer> headerMapping = new HashMap<>();

	private final List<String> columnNames;

	/**
	 * Creates a new instance of a container for row data.
	 *
	 * @param columnNames
	 *            the header row with the column names
	 */
	public DataRow(final List<String> columnNames) {
		this.columnNames = columnNames;
		for (int i = 0; i < columnNames.size(); i++) {
			final String columnName = columnNames.get(i);
			if (columnName != null) {
				this.headerMapping.put(columnName, i);
			}
		}
	}

	/**
	 * The count of columns (according to the header).
	 *
	 * @return the count of columns available
	 */
	public int getColumnCount() {
		return this.columnNames.size();
	}

	/**
	 * The name of column at the given index.
	 *
	 * @param columnIndex
	 *            the index of the column
	 * @return the name of the column or the empty string ("") if no column exists at the given index
	 */
	public String getName(final int columnIndex) {
		if (columnIndex < 0 || columnIndex >= this.columnNames.size()) {
			return "";
		}
		final String columnName = this.columnNames.get(columnIndex);
		return columnName == null ? "" : columnName;
	}

	/**
	 * Reads the value of the column at the given index.
	 *
	 * @param columnIndex
	 *            the index of the column
	 * @return the value in that column or the empty string (""), if the column is empty or does not exist
	 */
	public abstract String getValue(int columnIndex);

	/**
	 * Reads the value of the column with the given name.
	 *
	 * @param columnName
	 *            the name of the column.
	 * @return the value in that column or the empty string (""), if the column is empty or does not exist
	 */
	public String getValue(final String columnName) {
		final Integer columnIndex = this.headerMapping.get(columnName);
		if (columnIndex == null) {
			return "";
		}
		return getValue(columnIndex);
	}

}
