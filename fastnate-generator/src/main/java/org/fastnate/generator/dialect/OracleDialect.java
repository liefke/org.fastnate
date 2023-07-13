package org.fastnate.generator.dialect;

import java.io.IOException;
import java.util.Date;

import jakarta.persistence.TemporalType;

import org.fastnate.generator.RelativeDate;
import org.fastnate.generator.statements.StatementsWriter;

/**
 * Handles Oracle specific conversions.
 *
 * @see <a href="http://www.oracle.com/technetwork/indexes/documentation/index.html">Oracle - Documentation</a>
 * @author Tobias Liefke
 */
public class OracleDialect extends GeneratorDialect {

	private static final int MAX_VARCHAR_LENGTH = 2000;

	@Override
	public void adjustNextSequenceValue(final StatementsWriter writer, final String sequenceName,
			final long currentSequenceValue, final long nextSequenceValue, final int incrementSize) throws IOException {
		writer.writePlainStatement(this, "ALTER SEQUENCE " + sequenceName + " INCREMENT BY "
				+ (nextSequenceValue - currentSequenceValue - incrementSize));
		writer.writePlainStatement(this, "SELECT " + sequenceName + ".nextval FROM dual");
		writer.writePlainStatement(this, "ALTER SEQUENCE " + sequenceName + " INCREMENT BY " + incrementSize);
	}

	@Override
	public String buildCurrentSequenceValue(final String sequence, final int incrementSize, final boolean firstCall) {
		if (isEmulatingSequences()) {
			return super.buildCurrentSequenceValue(sequence, incrementSize, firstCall);
		}
		if (firstCall) {
			return "(SELECT last_number FROM user_sequences WHERE sequence_name = " + quoteString(sequence) + ')';
		}
		return sequence + ".currval";
	}

	@Override
	public String buildNextSequenceValue(final String sequence, final int incrementSize) {
		return sequence + ".nextval";
	}

	@Override
	public String convertTemporalValue(final Date value, final TemporalType type) {
		final String expression = super.convertTemporalValue(value, type);
		if (value instanceof RelativeDate.ReferenceDate || value instanceof RelativeDate) {
			return expression;
		}

		switch (type) {
			case DATE:
				return "to_date(" + expression + ", 'YYYY-MM-DD')";
			case TIME:
				return "to_date(" + expression + ", 'HH24:MI:SS')";
			case TIMESTAMP:
			default:
				return "to_timestamp(" + expression + ", 'YYYY-MM-DD HH24:MI:SS.FF9')";
		}
	}

	@Override
	protected String createAddDateExpression(final String referenceDate, final long value, final String unit) {
		final long abs = Math.abs(value);
		final int digits = (int) Math.ceil(Math.log10(abs));
		return referenceDate + ' ' + (value < 0 ? '-' : '+') + " INTERVAL '" + abs + "' " + unit + '(' + digits + ')';
	}

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("hextoraw('", blob, "')");
	}

	@Override
	public String getOptionalTable() {
		return "FROM DUAL";
	}

	@Override
	public boolean isEmptyStringEqualToNull() {
		return true;
	}

	@Override
	public boolean isFastInTransaction() {
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