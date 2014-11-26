package org.fastnate.generator.dialect;

import java.util.Date;

import javax.persistence.GenerationType;
import javax.persistence.TemporalType;

/**
 * Handles Oracle specific conversions.
 *
 * @see <a href="http://www.oracle.com/technetwork/indexes/documentation/index.html">Oracle - Documentation</a>
 * @author Tobias Liefke
 */
public final class OracleDialect extends GeneratorDialect {

	@Override
	public String buildCurrentSequenceValue(final String sequence) {
		return sequence + ".currval";
	}

	@Override
	public String buildNextSequenceValue(final String sequence) {
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
	public GenerationType getAutoGenerationType() {
		return GenerationType.SEQUENCE;
	}

	@Override
	public boolean isEmptyStringEqualToNull() {
		return true;
	}

	@Override
	public boolean isSequenceInWhereSupported() {
		return false;
	}
}