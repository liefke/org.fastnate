package org.fastnate.generator.provider;

import java.util.Properties;

/**
 * Encapsulates details specific to the current JPA implementation.
 *
 * @author Tobias Liefke
 */
public interface JpaProvider {

	/**
	 * The name of the default sequence, if none was specified for a sequence generator.
	 *
	 * @return the default sequence name
	 */
	String getDefaultSequence();

	/**
	 * Initializes this provider from the given settings.
	 *
	 * May as well change the settings according to some other settings found.
	 *
	 * @param settings
	 *            the settings of the generator context
	 */
	void initialize(Properties settings);

	/**
	 * Indicates if the current JPA provider needs always a discriminator column for a JOINED table.
	 *
	 * @return {@code true} to always write a discriminator column, {@code false} if it should be written only if
	 *         explicitly given
	 */
	boolean isJoinedDiscriminatorNeeded();

}
