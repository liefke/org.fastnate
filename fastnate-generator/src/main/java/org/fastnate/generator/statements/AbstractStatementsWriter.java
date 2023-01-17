package org.fastnate.generator.statements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.dialect.GeneratorDialect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Base class for implementations of {@link StatementsWriter}.
 *
 * @author Tobias Liefke
 */
public abstract class AbstractStatementsWriter implements StatementsWriter {

	/**
	 * Base class for insert and update statements.
	 *
	 * @author Tobias Liefke
	 */
	@Getter
	@RequiredArgsConstructor
	protected abstract static class AbstractTableStatement implements TableStatement {

		/** The current database dialect */
		private final GeneratorDialect dialect;

		/** The main table of this update / insert statement. */
		private final GeneratorTable table;

		/** The columns and their values. */
		private final Map<GeneratorColumn, ColumnExpression> values = new LinkedHashMap<>();

		/**
		 * Indicates that at least one of the {@link #values column expressions} is not a
		 * {@link PrimitiveColumnExpression primitive SQL value}.
		 */
		private boolean plainExpressionAvailable;

		/**
		 * Resets the content of this statement to reuse it.
		 */
		public void reset() {
			this.plainExpressionAvailable = false;
			this.values.clear();
		}

		/**
		 * Sets the value for a specific column.
		 *
		 * @param column
		 *            the column name
		 * @param value
		 *            the value expression
		 */
		@Override
		public void setColumnValue(final GeneratorColumn column, final ColumnExpression value) {
			if (value == null) {
				throw new NullPointerException("value should be not null");
			}
			if (this.values.put(column, value) != null) {
				throw new IllegalArgumentException("A value for " + column.getName() + " was assigned twice");
			}
			if (!this.plainExpressionAvailable && !(value instanceof PrimitiveColumnExpression)) {
				this.plainExpressionAvailable = true;
			}
		}

		@Override
		public String toString() {
			return toSql();
		}

	}

	/**
	 * Holds the information for an SQL Insert statement.
	 *
	 * @author Tobias Liefke
	 */
	@Getter
	protected static class InsertStatement extends AbstractTableStatement {

		/**
		 * Creates a new instance of InsertStatement.
		 *
		 * @param dialect
		 *            the current database dialect
		 * @param table
		 *            the affected table
		 */
		public InsertStatement(final GeneratorDialect dialect, final GeneratorTable table) {
			super(dialect, table);
		}

		/**
		 * Adds columns to an SQL expression.
		 *
		 * @param result
		 *            the string builder
		 * @param columns
		 *            contains the columns
		 * @return {@code result} for chaining
		 */
		protected StringBuilder addColumns(final StringBuilder result, final Collection<?> columns) {
			final Iterator<?> it = columns.iterator();
			if (it.hasNext()) {
				result.append(it.next());
			}
			while (it.hasNext()) {
				result.append(", ");
				result.append(it.next());
			}
			return result;
		}

		@Override
		public String toSql() {
			final StringBuilder result = new StringBuilder("INSERT INTO ").append(getTable().getQualifiedName());
			if (getValues().isEmpty()) {
				// Can happen if we have an generated identity column and only null values
				result.append(' ').append(getDialect().getEmptyValuesExpression());
			} else {
				result.append(" (");
				if (!getDialect().isSelectFromSameTableInInsertSupported() && isPlainExpressionAvailable()) {
					// Create MySQL compatible INSERTs
					final Pattern subselectPattern = Pattern.compile(
							"\\(SELECT\\s+(.*)\\s+FROM\\s+" + getTable().getQualifiedName() + "\\s*\\)",
							Pattern.CASE_INSENSITIVE);
					if (getValues().values().stream().anyMatch(e -> !(e instanceof PrimitiveColumnExpression)
							&& subselectPattern.matcher(e.toSql()).matches())) {
						addColumns(result, getValues().keySet()).append(") SELECT ");
						final List<String> rewrite = new ArrayList<>();
						for (final ColumnExpression value : getValues().values()) {
							final String sql = value.toSql();
							final Matcher matcher = subselectPattern.matcher(sql);
							if (matcher.matches()) {
								rewrite.add(matcher.group(1));
							} else {
								rewrite.add(sql);
							}
						}
						addColumns(result, rewrite).append(" FROM ").append(getTable().getQualifiedName());
						return result.toString();
					}
				}

				addColumns(result, getValues().keySet()).append(") VALUES (");
				for (final Iterator<ColumnExpression> iterator = getValues().values().iterator(); iterator.hasNext();) {
					iterator.next().appendSql(result);
					if (iterator.hasNext()) {
						result.append(", ");
					}
				}
				result.append(')');
			}
			return result.toString();
		}
	}

	/**
	 * A single (unparsed) SQL statement.
	 *
	 * @author Tobias Liefke
	 */
	@RequiredArgsConstructor
	protected static class PlainStatement implements EntityStatement {

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

	/**
	 * Holds the information for an SQL update statement.
	 *
	 * @author Tobias Liefke
	 */
	@Getter
	@Setter
	protected static class UpdateStatement extends AbstractTableStatement {

		private final GeneratorColumn idColumn;

		private final ColumnExpression idValue;

		/**
		 * Creates a new instance of UpdateStatement.
		 *
		 * @param dialect
		 *            the current database dialect
		 * @param table
		 *            the affected table
		 * @param idColumn
		 *            the column that contains the id of the changed entity
		 * @param idValue
		 *            the expression for the id of the changed entity
		 */
		protected UpdateStatement(final GeneratorDialect dialect, final GeneratorTable table,
				final GeneratorColumn idColumn, final ColumnExpression idValue) {
			super(dialect, table);
			this.idColumn = idColumn;
			this.idValue = idValue;
		}

		@Override
		public String toSql() {
			final StringBuilder result = new StringBuilder("UPDATE ").append(getTable().getQualifiedName())
					.append(" SET ");
			for (final Iterator<Map.Entry<GeneratorColumn, ColumnExpression>> entries = getValues().entrySet()
					.iterator(); entries.hasNext();) {
				final Entry<GeneratorColumn, ColumnExpression> entry = entries.next();
				result.append(entry.getKey().getName(getDialect())).append(" = ");
				entry.getValue().appendSql(result);
				if (entries.hasNext()) {
					result.append(", ");
				}
			}
			result.append(" WHERE ").append(this.idColumn.getName(getDialect())).append(" = ");
			this.idValue.appendSql(result);
			return result.toString();
		}

	}

	@Override
	public void close() throws IOException {
		// The default implementation does nothing
	}

	@Override
	public TableStatement createInsertStatement(final GeneratorDialect dialect, final GeneratorTable table) {
		return new InsertStatement(dialect, table);
	}

	@Override
	public EntityStatement createPlainStatement(final GeneratorDialect dialect, final String sql) {
		return new PlainStatement(sql);
	}

	@Override
	public TableStatement createUpdateStatement(final GeneratorDialect dialect, final GeneratorTable table,
			final GeneratorColumn idColumn, final ColumnExpression idValue) {
		return new UpdateStatement(dialect, table, idColumn, idValue);
	}

	@Override
	public void flush() throws IOException {
		// The default does nothing
	}

	@Override
	public void writeComment(final String comment) throws IOException {
		// The default implementation ignores every comment
	}

	@Override
	public void writeSectionSeparator() throws IOException {
		// The default implementation ignores every section separator
	}

}
