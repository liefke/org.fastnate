package org.fastnate.generator.context;

/**
 * Thrown if the classes in the entity model are for some reason invalid.
 *
 * @author Tobias Liefke
 */
public class ModelException extends RuntimeException {

	/**
	 * Throws a {@link ModelException} if the given condition is not met.
	 *
	 * @param condition
	 *            the condition to check
	 * @param errorMessage
	 *            the message to add to the exception, if the condition is not {@code true}
	 * @throws ModelException
	 *             if the condition is {@code false}
	 */
	public static void test(final boolean condition, final String errorMessage) {
		if (!condition) {
			throw new ModelException(errorMessage);
		}
	}

	private static final long serialVersionUID = 1L;

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
