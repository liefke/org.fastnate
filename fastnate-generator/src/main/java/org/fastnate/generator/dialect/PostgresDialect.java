package org.fastnate.generator.dialect;

/**
 * Handles PostgreSQL specific conversions.
 *
 * @see <a href="http://www.postgresql.org/docs/">PostgreSQL - Online manuals</a>
 */
public final class PostgresDialect extends GeneratorDialect {

	@Override
	protected void addQuotedCharacter(final StringBuilder result, final char c) {
		if (c == 0) {
			throw new IllegalArgumentException("PostgreSQL does not support '\\0' characters");
		}
		super.addQuotedCharacter(result, c);
	}

	@Override
	public String convertBooleanValue(final boolean value) {
		return value ? "true" : "false";
	}

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("decode('", blob, "', 'hex')");
	}

}