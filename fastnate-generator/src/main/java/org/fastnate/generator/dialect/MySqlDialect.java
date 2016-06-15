package org.fastnate.generator.dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

import com.google.common.base.Joiner;

/**
 * Handles MySQL specific conversions.
 *
 * Attention: MySQL is currently not fully supported. Especially
 * <ul>
 * <li>time and date functions</li>
 * </ul>
 * are not covered.
 *
 * @see <a href="http://dev.mysql.com/doc/">MySQL - Reference Manuals</a>
 *
 * @author Tobias Liefke
 */
public final class MySqlDialect extends GeneratorDialect {

	private static final char MAX_ESCAPE = '\\';

	private static final String[] ESCAPES = new String[MAX_ESCAPE + 1];

	private static final Joiner JOINER = Joiner.on(", ");

	static {
		ESCAPES['\0'] = "\\0";
		ESCAPES['\b'] = "\\b";
		ESCAPES['\t'] = "\\t";
		ESCAPES['\n'] = "\\n";
		ESCAPES['\r'] = "\\r";
		ESCAPES['\u0026'] = "\\Z";
		ESCAPES['\''] = "''";
		ESCAPES['\\'] = "\\\\";
	}

	/**
	 * Replace any subselect in an insert statement, if the same table is selected.
	 */
	@Override
	public String createSql(final EntityStatement stmt) {
		if (!(stmt instanceof InsertStatement)) {
			return stmt.toString();
		}
		final Map<String, String> values = stmt.getValues();
		if (values.isEmpty()) {
			return stmt.toString();
		}
		final Pattern subselectPattern = Pattern.compile("\\(SELECT\\s+(.*)\\s+FROM\\s+" + stmt.getTable() + "\\s*\\)",
				Pattern.CASE_INSENSITIVE);
		if (!subselectPattern.matcher(values.values().toString()).find()) {
			return stmt.toString();
		}

		final StringBuilder result = new StringBuilder("INSERT INTO ").append(stmt.getTable());
		result.append(" (");
		// Create MySQL compatible INSERTs
		JOINER.appendTo(result, values.keySet()).append(") SELECT ");
		final List<String> rewrite = new ArrayList<>();
		for (final String value : values.values()) {
			final Matcher matcher = subselectPattern.matcher(value);
			if (matcher.matches()) {
				rewrite.add(matcher.group(1));
			} else {
				rewrite.add(value);
			}
		}
		JOINER.appendTo(result, rewrite).append(" FROM ").append(stmt.getTable()).append(";\n");
		return result.toString();
	}

	/**
	 * Create MySQL specific quoting of the string.
	 */
	@Override
	public String quoteString(final String value) {
		if (value.length() == 0) {
			return "''";
		}
		final StringBuilder result = new StringBuilder(value.length() + 2).append('\'');
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c <= MAX_ESCAPE) {
				final String escape = ESCAPES[c];
				if (escape != null) {
					// Unprintable character, especially newlines
					result.append(escape);
					continue;
				}
			}
			result.append(c);
		}
		return result.append('\'').toString();
	}


	@Override
	public boolean isSequenceSupported() {
		return false;
	}
}