package org.fastnate.generator.dialect;

import java.util.Date;

import jakarta.persistence.TemporalType;

import org.fastnate.generator.RelativeDate;

/**
 * Handles MS SQL specific conversions.
 *
 * @see <a href="http://msdn.microsoft.com/de-de/sqlserver">MS SQL - MS Technet</a>
 * @author Tobias Liefke
 */
public class MsSqlDialect extends GeneratorDialect {

	@Override
	protected void addQuotedCharacter(final StringBuilder result, final char c) {
		result.append("CHAR(").append((byte) c).append(')');
	}

	@Override
	public String buildCurrentSequenceValue(final String sequence, final int incrementSize, final boolean firstCall) {
		if (isEmulatingSequences()) {
			return super.buildCurrentSequenceValue(sequence, incrementSize, firstCall);
		}
		return "(SELECT current_value FROM sys.sequences WHERE name = '" + sequence + "')";
	}

	@Override
	public String buildNextSequenceValue(final String sequence, final int incrementSize) {
		if (isEmulatingSequences()) {
			return super.buildNextSequenceValue(sequence, incrementSize);
		}
		return "NEXT VALUE FOR " + sequence;
	}

	@Override
	protected String convertTemporalValue(final Date value) {
		return super.convertTemporalValue(value).replace("-", "");
	}

	@Override
	public String convertTemporalValue(final Date value, final TemporalType type) {
		if (value == RelativeDate.TODAY) {
			return "CAST(GETDATE() AS DATE)";
		}
		return super.convertTemporalValue(value, type);
	}

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("0x", blob, "");
	}

	@Override
	public String getConcatOperator() {
		return " + ";
	}

	@Override
	public boolean isEmulatingSequences() {
		return true;
	}

	@Override
	public boolean isSettingIdentityAllowed() {
		return false;
	}

	@Override
	public String quoteIdentifier(final String name) {
		return '[' + name.replace("]", "]]") + ']';
	}

}