package org.fastnate.generator.dialect;

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

	@Override
	public boolean isInsertSelectSameTableSupported() {
		return false;
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

}