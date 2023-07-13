package org.fastnate.generator.dialect;

import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

import org.fastnate.generator.RelativeDate;
import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.statements.StatementsWriter;

import lombok.Getter;

/**
 * Handles database specific conversions.
 *
 * The conversions are limited, for some of the known limitations, see the database specific implementations.
 *
 * @see <a href="http://troels.arvin.dk/db/rdbms/">Comparison of different SQL implementations</a>
 *
 * @author Tobias Liefke
 */
@Getter
public abstract class GeneratorDialect {

	/**
	 * @deprecated Use {@link RelativeDate#NOW} instead
	 */
	@Deprecated
	public static final Date NOW = RelativeDate.NOW;

	private static void finishPart(final StringBuilder result, final String value, final int start, final int end,
			final boolean isOpen, final boolean close, final String concatOperator) {
		if (start < end) {
			if (!isOpen) {
				if (start > 0) {
					result.append(concatOperator);
				}
				result.append('\'');
			}
			result.append(value, start, end);
			if (close) {
				result.append('\'');
			}
		} else if (isOpen && close) {
			result.append('\'');
		} else if (!isOpen && !close) {
			if (start > 0) {
				result.append(concatOperator);
			}
			result.append('\'');
		}
	}

	private final char[] letter = "0123456789ABCDEF".toCharArray();

	/**
	 * Adds a quoted character to the result string buffer.
	 *
	 * @param result
	 *            the current result buffer
	 * @param c
	 *            the character to quote
	 */
	protected void addQuotedCharacter(final StringBuilder result, final char c) {
		result.append("CHR(").append((byte) c).append(')');
	}

	/**
	 * Adjusts the next value of the given identity column to ensure that it is bigger than the last generated value.
	 *
	 * @param writer
	 *            the target of any generated statement
	 * @param table
	 *            the table of the column
	 * @param column
	 *            the (auto increment) identity column
	 * @param nextValue
	 *            the next value of the identity column
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void adjustNextIdentityValue(final StatementsWriter writer, final GeneratorTable table,
			final GeneratorColumn column, final long nextValue) throws IOException {
		// Most of the dialects do nothing
	}

	/**
	 * Adjusts the given sequence to ensure that the next value is exactly the given value.
	 *
	 * @param writer
	 *            the target of the generated statements
	 * @param sequenceName
	 *            the fully qualified name of the sequence
	 * @param currentSequenceValue
	 *            the current value of the sequence
	 * @param nextSequenceValue
	 *            the next value of the sequence
	 * @param incrementSize
	 *            the increment size of the sequence
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void adjustNextSequenceValue(final StatementsWriter writer, final String sequenceName,
			final long currentSequenceValue, final long nextSequenceValue, final int incrementSize) throws IOException {
		if (isEmulatingSequences()) {
			writer.writePlainStatement(this, "UPDATE " + sequenceName + " SET next_val = " + nextSequenceValue);
		} else {
			writer.writePlainStatement(this, "ALTER SEQUENCE " + sequenceName + " RESTART WITH " + nextSequenceValue);
		}
	}

	/**
	 * Builds the SQL expression that is used for referencing the current value of the given sequence.
	 *
	 * @param sequence
	 *            the fully qualified name of the sequence
	 * @param incrementSize
	 *            the expected incrementSize, as given in the schema - used by some dialects to ensure that exactly that
	 *            inrement is used
	 * @param firstCall
	 *            indicates that the sequence was not updated before in this session - we may need to use a different
	 *            approach to get the current value
	 * @return the SQL expression to use in statement
	 */
	public String buildCurrentSequenceValue(final String sequence, final int incrementSize, final boolean firstCall) {
		if (isEmulatingSequences()) {
			return "(SELECT max(next_val) - " + incrementSize + " FROM " + sequence + ")";
		}
		return "currval('" + sequence + "')";
	}

	/**
	 * Builds the SQL expression resp. statement that is used for creating the next value of the given sequence.
	 *
	 * Depending on {@link #isNextSequenceValueInInsertSupported()} this will return an expression or a statement.
	 *
	 * @param sequence
	 *            the fully qualified name of the sequence
	 * @param incrementSize
	 *            the expected incrementSize, as given in the schema - used by some dialects to ensure that exactly that
	 *            inrement is used
	 * @return the SQL to use in resp. before the insert / update statement
	 */
	public String buildNextSequenceValue(final String sequence, final int incrementSize) {
		if (isEmulatingSequences()) {
			return "UPDATE " + sequence + " SET next_val = next_val + " + incrementSize;
		}
		return "nextval('" + sequence + "')";
	}

	/**
	 * Converts a boolean value to an SQL expression for the current database type.
	 *
	 * @param value
	 *            the value to convert
	 * @return the SQL representation of the value
	 */
	public String convertBooleanValue(final boolean value) {
		return value ? "1" : "0";
	}

	/**
	 * Converts a numeric value to an SQL expression for the current database type.
	 *
	 * @param value
	 *            the numeric value
	 * @return the SQL expression of that value
	 */
	public String convertNumberValue(final Number value) {
		return String.valueOf(value);
	}

	/**
	 * Builds the SQL expression for a date / time / timestamp.
	 *
	 * @param sqlDate
	 *            the date, already converted to one from {@code jakarta.sql.*}
	 * @return the SQL expression representing the value.
	 */
	protected String convertTemporalValue(final Date sqlDate) {
		return '\'' + sqlDate.toString() + '\'';
	}

	/**
	 * Converts a date to an appropriate SQL expression.
	 *
	 * @param value
	 *            the timestamp value
	 * @param type
	 *            the type
	 * @return SQL expression representing the value.
	 */
	public String convertTemporalValue(final Date value, final TemporalType type) {
		if (RelativeDate.NOW.equals(value)) {
			return "CURRENT_TIMESTAMP";
		}
		if (RelativeDate.TODAY.equals(value)) {
			return "CURRENT_DATE";
		}
		if (value instanceof RelativeDate) {
			final RelativeDate relativeDate = (RelativeDate) value;
			final long difference = relativeDate.getDifference();
			final RelativeDate.Precision precision = relativeDate.getPrecision();
			return createAddDateExpression(convertTemporalValue(((RelativeDate) value).getReferenceDate(), type),
					difference / precision.getMillis(), precision.getUnit());
		}
		return convertTemporalValue(convertToDatabaseDate(value, type));
	}

	/**
	 * Converts any {@link Date} value to a {@link java.sql} specific date object.
	 *
	 * @param value
	 *            the Java-Date object
	 * @param type
	 *            the type of the conversion
	 * @return the corresponding java.sql specific date object
	 */
	public Date convertToDatabaseDate(final Date value, final TemporalType type) {
		Date actualTime = value;
		if (value instanceof RelativeDate) {
			final RelativeDate relativeDate = (RelativeDate) value;
			if (RelativeDate.NOW == relativeDate.getReferenceDate()) {
				actualTime = new Date(System.currentTimeMillis() + relativeDate.getDifference());
			}
		} else if (RelativeDate.NOW.equals(value)) {
			actualTime = new Date(System.currentTimeMillis());
		}
		switch (type) {
			case DATE:
				return actualTime instanceof java.sql.Date ? (java.sql.Date) actualTime
						: new java.sql.Date(actualTime.getTime());
			case TIME:
				return actualTime instanceof Time ? (Time) actualTime : new Time(actualTime.getTime());
			case TIMESTAMP:
			default:
				return actualTime instanceof Timestamp ? (Timestamp) actualTime : new Timestamp(actualTime.getTime());
		}
	}

	/**
	 * Creates an SQL expression to add a value to a date.
	 *
	 * @param referenceDate
	 *            the expression for the reference date
	 * @param value
	 *            the value to add to the date
	 * @param unit
	 *            the unit of the value
	 * @return the SQL expression that adds the value
	 */
	protected String createAddDateExpression(final String referenceDate, final long value, final String unit) {
		return "DATEADD(" + unit + ", " + value + ", " + referenceDate + ')';
	}

	/**
	 * Converts the given byte array to an SQL expression for the current database.
	 *
	 * @param blob
	 *            the bytes to convert
	 * @return the expression for the bytes
	 */
	public String createBlobExpression(final byte[] blob) {
		throw new IllegalArgumentException("Blobs are not supported by " + getClass().getSimpleName());
	}

	/**
	 * Creates the hex presentation of the given blob.
	 *
	 * @param prefix
	 *            the prefix to add to the hex
	 * @param blob
	 *            the binary blob to convert
	 * @param suffix
	 *            the suffix to add to the hex
	 * @return prefix + hex(blob) + suffix
	 */
	protected String createHexBlobExpression(final String prefix, final byte[] blob, final String suffix) {
		final int start = prefix.length();
		final int suffixStart = start + blob.length * 2;
		final char[] result = new char[suffixStart + suffix.length()];
		prefix.getChars(0, start, result, 0);

		// CHECKSTYLE OFF: MagicNumber - its better to read without constants
		for (int i = 0; i < blob.length; i++) {
			final int v = blob[i] & 0xFF;
			result[i * 2 + start] = this.letter[v >>> 4];
			result[i * 2 + start + 1] = this.letter[v & 0x0F];
		}
		// CHECKSTYLE ON

		suffix.getChars(0, suffix.length(), result, suffixStart);
		return new String(result);
	}

	/**
	 * Resolves the GenerationType used, if {@link GenerationType#AUTO} is set for a {@link GeneratedValue}.
	 *
	 * @return the replacement for {@link GenerationType#AUTO} for the current dialect in Hibernate.
	 */
	public GenerationType getAutoGenerationType() {
		return isSequenceSupported() ? GenerationType.SEQUENCE
				: isIdentitySupported() ? GenerationType.IDENTITY : GenerationType.TABLE;
	}

	/**
	 * The operator used to concat two Strings.
	 *
	 * @return the operator to use for "string1 OPERATOR string2" (including any necessary whitespace) or {@code null}
	 *         if this dialect does not have such an operator
	 */
	public String getConcatOperator() {
		return " || ";
	}

	/**
	 * The SQL expression to use in an empty insert statement.
	 *
	 * @return the SQL expression if an insert statement contains no values
	 */
	public Object getEmptyValuesExpression() {
		return "DEFAULT VALUES";
	}

	/**
	 * Returns the string to use when no table is required, e.g. for "SELECT 1, 2 FROM DUAL" this would return "FROM
	 * DUAL".
	 *
	 * @return the SQL to use in "SELECT x [OPTIONALTABLE] WHERE ..."
	 */
	public String getOptionalTable() {
		return "";
	}

	/**
	 * Indicates that the empty string is equal to {@code null} in this database.
	 *
	 * Important for {@link NotNull} constraints, where an empty string would result in the same constraint violation.
	 *
	 * @return {@code true} if this database assumes that an empty string is the same as {@code null}
	 */
	public boolean isEmptyStringEqualToNull() {
		return false;
	}

	/**
	 * Indicates that tables are used in place of sequences, if {@link GenerationType#SEQUENCE} is defined for a
	 * {@link GeneratedValue}.
	 *
	 * @return {@code true} if sequences are emulated with tables, {@code false} if sequences are supported
	 */
	public boolean isEmulatingSequences() {
		return false;
	}

	/**
	 * Indicates that the database usually faster when all statements are executed within an transaction.
	 *
	 * For example Oracle is approx. 25% faster but H2 is 200% slower compared to auto commit.
	 *
	 * Only relevant if executed against a running database.
	 *
	 * @return {@code true} if an transaction is faster than auto commit
	 */
	public boolean isFastInTransaction() {
		return false;
	}

	/**
	 * Indicates that identity columns are supported by the database.
	 *
	 * @return {@code true} if the database supports identities
	 */
	public boolean isIdentitySupported() {
		return true;
	}

	/**
	 * Indicates that a sequence may be updated in the insert statement.
	 *
	 * @return {@code true} to indicate that {@link #buildNextSequenceValue(String,int)} will return an expression that
	 *         may be used in an INSERT statement, {@code false} to indicate that
	 *         {@link #buildNextSequenceValue(String,int)} returns a full statement
	 */
	public boolean isNextSequenceValueInInsertSupported() {
		return !isEmulatingSequences();
	}

	/**
	 * Indicates that this dialect supports the {@link Table#schema() schema} naming.
	 *
	 * @return {@code true} if we support the name of schema when referencing a table
	 */
	public boolean isSchemaSupported() {
		return true;
	}

	/**
	 * Indicates that this dialect may select from the same table in an insert statement.
	 *
	 * @return {@code true} if "INSERT INTO MY_TABLE (a) VALUES (SELECT max(a) FROM MY_TABLE)" is supported
	 */
	public boolean isSelectFromSameTableInInsertSupported() {
		return true;
	}

	/**
	 * Indicates that references to sequences in {@code WHERE} expressions are supported.
	 *
	 * @return {@code true} if this database supports sequences in {@code WHERE} expressions
	 */
	public boolean isSequenceInWhereSupported() {
		return isSequenceSupported();
	}

	/**
	 * Indicates that sequences are supported by the database.
	 *
	 * @return {@code true} if the database supports sequences
	 */
	public boolean isSequenceSupported() {
		return true;
	}

	/**
	 * Indicates if this dialect supports writing absolute values to an {@link GenerationType#IDENTITY} column.
	 *
	 * @return {@code true} if we can write fix values for identity columns
	 */
	public boolean isSettingIdentityAllowed() {
		return true;
	}

	/**
	 * Quotes an object name for the current database dialect.
	 *
	 * @param name
	 *            the plain object name
	 *
	 * @return the quoted object name
	 */
	public String quoteIdentifier(final String name) {
		return '"' + name.replace("\"", "\"\"") + '"';
	}

	/**
	 * Quotes the given string.
	 *
	 * @param value
	 *            the value to quote
	 * @return the quoted string
	 */
	public String quoteString(final String value) {
		if (value.length() == 0) {
			return "''";
		}
		final StringBuilder result = new StringBuilder(value.length() + 2);
		int start = 0;
		boolean isOpen = false;
		final String concatOperator = getConcatOperator();
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c < ' ') {
				// Unprintable character, especially newlines
				if (i > 0) {
					finishPart(result, value, start, i, isOpen, true, concatOperator);
					isOpen = false;
					result.append(concatOperator);
				}
				addQuotedCharacter(result, c);
			} else if (c == '\'') {
				// Escape quotes
				finishPart(result, value, start, i, isOpen, false, concatOperator);
				isOpen = true;
				result.append("''");
			} else {
				continue;
			}
			start = i + 1;
		}
		finishPart(result, value, start, value.length(), isOpen, true, concatOperator);
		return result.toString();
	}

	/**
	 * Adds a "truncate table" statement to the given writer.
	 *
	 * @param writer
	 *            the target of the statement
	 * @param table
	 *            the table to truncate
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void truncateTable(final StatementsWriter writer, final GeneratorTable table) throws IOException {
		writer.writePlainStatement(this, "TRUNCATE TABLE " + table.getQualifiedName());
	}
}
