package org.fastnate.generator.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import org.fastnate.generator.dialect.GeneratorDialect;

import com.google.common.base.Joiner;

/**
 * Holds the information for an SQL Insert statement.
 *
 * @author Tobias Liefke
 */
@Getter
public class InsertStatement extends EntityStatement {

	private static final Joiner JOINER = Joiner.on(", ");

	private final Pattern subselectPattern;

	/**
	 * Creates a new instance of InsertStatement.
	 *
	 * @param table
	 *            the affected table
	 * @param dialect
	 *            the current database dialect
	 */
	public InsertStatement(final String table, final GeneratorDialect dialect) {
		super(table);
		if (dialect.isInsertSelectSameTableSupported()) {
			this.subselectPattern = null;
		} else {
			// Create MySQL compatible INSERTs
			this.subselectPattern = Pattern.compile("\\(SELECT\\s+(.*)\\s+FROM\\s+" + getTable() + "\\s*\\)",
					Pattern.CASE_INSENSITIVE);
		}
	}

	/**
	 * Creates the SQL for this statement.
	 *
	 * @return the resulting SQL
	 */
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder("INSERT INTO ").append(getTable()).append(" (");
		if (this.subselectPattern != null && this.subselectPattern.matcher(getValues().values().toString()).find()) {
			// Create MySQL compatible INSERTs
			JOINER.appendTo(result, getValues().keySet()).append(") SELECT ");
			final List<String> rewrite = new ArrayList<>();
			for (final String value : getValues().values()) {
				final Matcher matcher = this.subselectPattern.matcher(value);
				if (matcher.matches()) {
					rewrite.add(matcher.group(1));
				} else {
					rewrite.add(value);
				}
			}
			JOINER.appendTo(result, rewrite).append(" FROM ").append(getTable()).append(";\n");
		} else {
			JOINER.appendTo(result, getValues().keySet()).append(") VALUES (");
			JOINER.appendTo(result, getValues().values()).append(");\n");
		}
		return result.toString();
	}
}
