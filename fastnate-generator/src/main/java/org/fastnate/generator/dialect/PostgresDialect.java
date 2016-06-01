package org.fastnate.generator.dialect;

/**
 * Handles PostgreSQL specific conversions.
 *
 * @see <a href="http://www.postgresql.org/docs/">PostgreSQL - Online manuals</a>
 */
public final class PostgresDialect extends GeneratorDialect {

	@Override
	public String convertBooleanValue(final boolean value) {
		return value ? "true" : "false";
	}

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("decode('", blob, "', 'hex')");
	}

	@Override
	public boolean isIdentitySupported() {
		return false;
	}

}