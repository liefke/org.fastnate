package org.fastnate.generator.dialect;

import java.util.Date;

import javax.persistence.TemporalType;

/**
 * Handles Oracle specific conversions.
 *
 * @see <a href="http://www.oracle.com/technetwork/indexes/documentation/index.html">Oracle - Documentation</a>
 * @author Tobias Liefke
 */
public final class OracleDialect extends GeneratorDialect {

	private static final int MAX_VARCHAR_LENGTH = 2000;

	@Override
	public String buildCurrentSequenceValue(final String sequence) {
		return sequence + ".currval";
	}

	@Override
	public String buildNextSequenceValue(final String sequence, final int incrementSize) {
		return sequence + ".nextval";
	}

	@Override
	public String convertTemporalValue(final Date value, final TemporalType type) {
		if (NOW.equals(value)) {
			return "CURRENT_TIMESTAMP";
		}
		switch (type) {
		case DATE:
			return "to_date(" + super.convertTemporalValue(value, type) + ", 'YYYY-MM-DD')";
		case TIME:
			return "to_date(" + super.convertTemporalValue(value, type) + ", 'HH24:MI:SS')";
		case TIMESTAMP:
		default:
			return "to_timestamp(" + super.convertTemporalValue(value, type) + ", 'YYYY-MM-DD HH24:MI:SS.FF9')";
		}
	}

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("hextoraw('", blob, "')");
	}

	@Override
	public boolean isEmptyStringEqualToNull() {
		return true;
	}

	@Override
	public boolean isIdentitySupported() {
		return false;
	}

	@Override
	public boolean isSequenceInWhereSupported() {
		return false;
	}

	@Override
	public String quoteString(final String value) {
		if (value.length() > MAX_VARCHAR_LENGTH) {
			// If our string is to long, we use "TO_CLOB" to ensure that the content fits
			final StringBuilder result = new StringBuilder();
			for (int i = 0; i < value.length(); i += MAX_VARCHAR_LENGTH) {
				if (i > 0) {
					result.append(" || ");
				}
				result.append("TO_CLOB(")
						.append(super.quoteString(value.substring(i, Math.min(value.length(), i + MAX_VARCHAR_LENGTH))))
						.append(')');
			}
			return result.toString();
		}
		return super.quoteString(value);
	}
}