package org.fastnate.generator.context;

/**
 * Thrown if the classes in the entity model are for some reason invalid.
 *
 * @author Tobias Liefke
 */
public class ModelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Builds the error message from the given message and parameters.
	 * 
	 * @param errorMessage
	 *            the message, each of <code>{}</code> is replaced by one of the given parameters
	 * @param parameters
	 *            the optional parameters for the error message
	 * @return the error message
	 */
	public static String buildErrorMessage(final String errorMessage, final Object... parameters) {
		// Use simple string concatenation in loop - as our exception is thrown only once
		// and readabilty beats performance here
		int last = 0;
		String error = errorMessage;
		for (final Object parameter : parameters) {
			final int next = error.indexOf("{}", last);
			if (next < 0) {
				break;
			}
			final String paramString = String.valueOf(parameter);
			error = error.substring(0, next) + paramString + error.substring(next + 2);
			last = next + paramString.length();
		}
		return error;
	}

	/**
	 * Throws a {@link ModelException} if the given value is {@code null}.
	 *
	 * @param value
	 *            the value that should not be {@code null}
	 * @param errorMessage
	 *            the message to include in the exception, if the value is {@code null}, each of <code>{}</code> is
	 *            replaced by one of the given parameters
	 * @param parameters
	 *            the optional parameters for the error message
	 * @throws ModelException
	 *             if the value is {@code null}
	 */
	public static void mustExist(final Object value, final String errorMessage, final Object... parameters) {
		test(value != null, errorMessage, parameters);
	}

	/**
	 * Throws a {@link ModelException} if the given condition is not met.
	 *
	 * @param condition
	 *            the condition to check
	 * @param errorMessage
	 *            the message to include in the exception, if the condition is not {@code true}, each of <code>{}</code>
	 *            is replaced by one of the given parameters
	 * @param parameters
	 *            the optional parameters for the error message
	 * @throws ModelException
	 *             if the condition is {@code false}
	 */
	public static void test(final boolean condition, final String errorMessage, final Object... parameters) {
		if (!condition) {
			throw new ModelException(buildErrorMessage(errorMessage, parameters));
		}
	}

	/**
	 * Creates a new instance of {@link ModelException}.
	 *
	 * @param message
	 *            the message of the exception
	 */
	public ModelException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance of {@link ModelException}.
	 *
	 * @param message
	 *            the detail message of the exception
	 * @param cause
	 *            the origin of this exception
	 */
	public ModelException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
