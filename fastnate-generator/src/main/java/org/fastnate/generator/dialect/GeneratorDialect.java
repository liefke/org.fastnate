package org.fastnate.generator.dialect;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.PlainStatement;

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
	 * Represents the constant for writing the "now" function to SQL.
	 */
	public static final Date NOW = new Date();

	private static void finishPart(final StringBuilder result, final String value, final int start, final int end,
			final boolean isOpen, final boolean close) {
		if (start < end) {
			if (!isOpen) {
				if (start > 0) {
					result.append(" || ");
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
				result.append(" || ");
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
	 * @param tableName
	 *            the name of the table of the column
	 * @param columnName
	 *            the name of the (auto increment) identity column
	 * @param nextValue
	 *            the next value of the identity column
	 * @return all statements that are necessary to adjust the next value
	 */
	public List<? extends EntityStatement> adjustNextIdentityValue(final String tableName, final String columnName,
			final long nextValue) {
		// Most of the dialects do nothing
		return Collections.emptyList();
	}

	/**
	 * Adjusts the given sequence to ensure that the next value is exactly the given value.
	 *
	 * @param sequenceName
	 *            the name of the sequence
	 * @param currentSequenceValue
	 *            the current value of the sequence
	 * @param nextSequenceValue
	 *            the next value of the sequence
	 * @param incrementSize
	 *            the increment size of the sequence
	 * @return all statements necessary to adjust the sequence
	 */
	public List<? extends EntityStatement> adjustNextSequenceValue(final String sequenceName,
			final long currentSequenceValue, final long nextSequenceValue, final int incrementSize) {
		return Collections.singletonList(
				new PlainStatement("ALTER SEQUENCE " + sequenceName + " RESTART WITH " + nextSequenceValue));
	}

	/**
	 * Builds the SQL expression that is used for referencing the current value of the given sequence.
	 *
	 * @param sequence
	 *            the name of the sequence
	 * @param incrementSize
	 *            the expected incrementSize, as given in the schema - used by some dialects to ensure that exactly that
	 *            inrement is used
	 * @return the SQL expression to use in statement
	 */
	public String buildCurrentSequenceValue(final String sequence, final int incrementSize) {
		return "currval('" + sequence + "')";
	}

	/**
	 * Builds the SQL expression resp. statement that is used for creating the next value of the given sequence.
	 *
	 * Depending on {@link #isNextSequenceValueInInsertSupported()} this will return an expression or a statement.
	 *
	 * @param sequence
	 *            the name of the sequence
	 * @param incrementSize
	 *            the expected incrementSize, as given in the schema - used by some dialects to ensure that exactly that
	 *            inrement is used
	 * @return the SQL to use in resp. before the insert / update statement
	 */
	public String buildNextSequenceValue(final String sequence, final int incrementSize) {
		return "nextval('" + sequence + "')";
	}

	/**
	 * Converts a boolean value for the current database type.
	 *
	 * @param value
	 *            the value to convert
	 * @return the string representation of the value
	 */
	public String convertBooleanValue(final boolean value) {
		return value ? "1" : "0";
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
		if (NOW.equals(value)) {
			return "CURRENT_TIMESTAMP";
		}
		final Date date;
		switch (type) {
			case DATE:
				date = value instanceof java.sql.Date ? (java.sql.Date) value : new java.sql.Date(value.getTime());
				break;
			case TIME:
				date = value instanceof Time ? (Time) value : new Time(value.getTime());
				break;
			case TIMESTAMP:
			default:
				date = value instanceof Timestamp ? (Timestamp) value : new Timestamp(value.getTime());
		}
		return '\'' + date.toString() + '\'';
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
	 * Creates an SQL statement from the given insert statement.
	 *
	 * Usually just {@link EntityStatement#toString()} is returned, but some dialects could change database specific
	 * things.
	 *
	 * @param stmt
	 *            contains the table and all column values
	 * @return the SQL
	 */
	public String createSql(final EntityStatement stmt) {
		return stmt.toSql();
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
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c < ' ' && c != '\t') {
				// Unprintable character, especially newlines
				if (i > 0) {
					finishPart(result, value, start, i, isOpen, true);
					isOpen = false;
					result.append(" || ");
				}
				addQuotedCharacter(result, c);
			} else if (c == '\'') {
				// Escape quotes
				finishPart(result, value, start, i, isOpen, false);
				isOpen = true;
				result.append("''");
			} else {
				continue;
			}
			start = i + 1;
		}
		finishPart(result, value, start, value.length(), isOpen, true);
		return result.toString();
	}
}
