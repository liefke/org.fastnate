package org.fastnate.generator.dialect;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.fastnate.generator.statements.EntityStatement;

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
	 * Builds the SQL expression that is used for referencing the current value of the given sequence.
	 *
	 * @param sequence
	 *            the name of the sequence
	 * @return the SQL expression to use in statement
	 */
	public String buildCurrentSequenceValue(final String sequence) {
		return "currval('" + sequence + "')";
	}

	/**
	 * Builds the SQL expression that is used for creating the next value of the given sequence.
	 *
	 * @param sequence
	 *            the name of the sequence
	 * @return the SQL to use in an insert / update statement
	 */
	public String buildNextSequenceValue(final String sequence) {
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
		return stmt.toString();
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
	 * Indicates that references to sequences in {@code WHERE} expressions are supported.
	 *
	 * @return {@code true} if this database supports sequences in {@code WHERE} expressions
	 */
	public boolean isSequenceInWhereSupported() {
		return true;
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
				result.append("CHR(").append((byte) c).append(')');
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
