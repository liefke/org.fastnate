package org.fastnate.generator.provider;

/**
 * Encapsulates details specific to the current JPA implementation.
 *
 * @author Tobias Liefke
 */
public interface JpaProvider {

	/**
	 * Indicates if the current JPA provider needs always a discriminator column for a JOINED table.
	 *
	 * @return {@code true} to always write a discriminator column, {@code false} if it should be written only if
	 *         explicitly given
	 */
	boolean isJoinedDiscriminatorNeeded();

	/**
	 * The name of the default sequence, if none was specified for a sequence generator.
	 * 
	 * @return the default sequence name
	 */
	String getDefaultSequence();
}
