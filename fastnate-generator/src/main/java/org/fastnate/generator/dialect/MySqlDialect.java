package org.fastnate.generator.dialect;

/**
 * Handles MySQL specific conversions.
 *
 * @see <a href="http://dev.mysql.com/doc/">MySQL - Reference Manuals</a>
 *
 * @author Tobias Liefke
 */
public class MySqlDialect extends GeneratorDialect {

	private static final char MAX_ESCAPE = '\\';

	private static final String[] ESCAPES = new String[MAX_ESCAPE + 1];

	static {
		ESCAPES['\0'] = "\\0";
		ESCAPES['\b'] = "\\b";
		ESCAPES['\t'] = "\\t";
		ESCAPES['\n'] = "\\n";
		ESCAPES['\r'] = "\\r";
		ESCAPES['\u001A'] = "\\Z";
		ESCAPES['\''] = "''";
		ESCAPES['\\'] = "\\\\";
	}

	@Override
	protected String createAddDateExpression(final String referenceDate, final long value, final String unit) {
		return "DATE_ADD(" + referenceDate + ", INTERVAL " + value + ' ' + unit + ')';
	}

	/**
	 * Create MySQL specific binary expression.
	 */
	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("x'", blob, "'");
	}

	@Override
	public Object getEmptyValuesExpression() {
		return "VALUES ()";
	}

	@Override
	public String getOptionalTable() {
		return "FROM DUAL";
	}

	@Override
	protected boolean isEmulatingSequences() {
		return true;
	}

	@Override
	public boolean isFastInTransaction() {
		return true;
	}

	@Override
	public boolean isSelectFromSameTableInInsertSupported() {
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