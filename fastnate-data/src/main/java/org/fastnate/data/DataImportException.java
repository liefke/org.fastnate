package org.fastnate.data;

import lombok.Getter;

/**
 * A runtime exception that can be used by all data providers to indicate the exact position of an error during import
 * of data.
 *
 * @author Tobias Liefke
 */
@Getter
public class DataImportException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** The optional file name that had the error. */
	private final String file;

	/** The optional line that had the error (-1 if unknown). */
	private final int line;

	/** The optional column that had the error (-1 if unknown). */
	private final int column;

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 */
	public DataImportException(final String message) {
		this(message, null, -1, -1);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param file
	 *            the optional file name that had the error
	 */
	public DataImportException(final String message, final String file) {
		this(message, file, -1, -1);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param file
	 *            the optional file name that had the error
	 * @param line
	 *            the optional line that had the error (-1 if unknown)
	 */
	public DataImportException(final String message, final String file, final int line) {
		this(message, file, line, -1);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param file
	 *            the optional file name that had the error
	 * @param line
	 *            the optional line that had the error (-1 if unknown)
	 * @param column
	 *            the optional column that had the error (-1 if unknown)
	 */
	public DataImportException(final String message, final String file, final int line, final int column) {
		super(message);
		this.file = file;
		this.line = line;
		this.column = column;
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param file
	 *            the optional file name that had the error
	 * @param line
	 *            the optional line that had the error (-1 if unknown)
	 * @param column
	 *            the optional column that had the error (-1 if unknown)
	 * @param cause
	 *            the cause of the error
	 */
	public DataImportException(final String message, final String file, final int line, final int column,
			final Throwable cause) {
		super(message, cause);
		this.file = file;
		this.line = line;
		this.column = column;
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param file
	 *            the optional file name that had the error
	 * @param line
	 *            the optional line that had the error (-1 if unknown)
	 * @param cause
	 *            the cause of the error
	 */
	public DataImportException(final String message, final String file, final int line, final Throwable cause) {
		this(message, file, line, -1, cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param file
	 *            the optional file name that had the error
	 * @param cause
	 *            the cause of the error
	 */
	public DataImportException(final String message, final String file, final Throwable cause) {
		this(message, file, -1, -1, cause);
	}

	/**
	 * Creates a new exception.
	 *
	 * @param message
	 *            the error message
	 * @param cause
	 *            the cause of the error
	 */
	public DataImportException(final String message, final Throwable cause) {
		this(message, null, -1, -1, cause);
	}

	@Override
	public String toString() {
		final String result = super.toString();
		if (this.file != null || this.line >= 0 || this.column >= 0) {
			return result + " (" + (this.file != null ? this.file : "") + (this.line >= 0 ? ":" + this.line : "")
					+ (this.column >= 0 ? ":" + this.column : "") + ')';
		}
		return result;
	}

}
