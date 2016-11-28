package org.fastnate.generator.dialect;

import java.util.Collections;
import java.util.List;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.PlainStatement;

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
	public List<? extends EntityStatement> adjustNextIdentityValue(final String tableName, final String columnName,
			final long nextValue) {
		return Collections
				.singletonList(new PlainStatement("ALTER SEQUENCE " + tableName + "_id_seq RESTART WITH " + nextValue));
	}

	@Override
	public String convertBooleanValue(final boolean value) {
		return value ? "true" : "false";
	}

	@Override
	protected String createAddDateExpression(final String referenceDate, final long value, final String unit) {
		return referenceDate + ' ' + (value < 0 ? '-' : '+') + " INTERVAL '" + Math.abs(value) + ' ' + unit + '\'';
	}

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("decode('", blob, "', 'hex')");
	}

}