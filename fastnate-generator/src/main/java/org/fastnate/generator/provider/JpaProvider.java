package org.fastnate.generator.provider;

import java.util.Properties;

import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

/**
 * Encapsulates details specific to the current JPA implementation.
 *
 * @author Tobias Liefke
 */
public interface JpaProvider {

	/**
	 * The name of the default generator {@link TableGenerator#table()}, if none was specified for a table generator.
	 *
	 * @return the default generator table
	 */
	String getDefaultGeneratorTable();

	/**
	 * The name of the default generator {@link TableGenerator#pkColumnName()}, if none was specified for a table
	 * generator.
	 *
	 * @return the default primary column name for the generator table
	 */
	String getDefaultGeneratorTablePkColumnName();

	/**
	 * The name of the default generator {@link TableGenerator#pkColumnValue()}, if none was specified for a table
	 * generator.
	 *
	 * @return the default primary column value for the generator table
	 */
	String getDefaultGeneratorTablePkColumnValue();

	/**
	 * The name of the default generator {@link TableGenerator#valueColumnName()}, if none was specified for a table
	 * generator.
	 *
	 * @return the default value column name for the generator table
	 */
	String getDefaultGeneratorTableValueColumnName();

	/**
	 * The name of the default {@link SequenceGenerator#sequenceName() sequence}, if none was specified for a sequence
	 * generator.
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
