package org.fastnate.generator.dialect;

import java.io.IOException;

import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.statements.StatementsWriter;

/**
 * Handles PostgreSQL specific conversions.
 *
 * @see <a href="http://www.postgresql.org/docs/">PostgreSQL - Online manuals</a>
 */
public class PostgresDialect extends GeneratorDialect {

	@Override
	protected void addQuotedCharacter(final StringBuilder result, final char c) {
		if (c == 0) {
			throw new IllegalArgumentException("PostgreSQL does not support '\\0' characters");
		}
		super.addQuotedCharacter(result, c);
	}

	@Override
	public void adjustNextIdentityValue(final StatementsWriter writer, final GeneratorTable table,
			final GeneratorColumn columnName, final long nextValue) throws IOException {
		writer.writePlainStatement(this,
				"ALTER SEQUENCE " + table.getQualifiedName() + "_id_seq RESTART WITH " + nextValue);
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

	@Override
	public void truncateTable(final StatementsWriter writer, final GeneratorTable table) throws IOException {
		writer.writePlainStatement(this, "TRUNCATE TABLE " + table.getQualifiedName() + " CASCADE");
	}

}