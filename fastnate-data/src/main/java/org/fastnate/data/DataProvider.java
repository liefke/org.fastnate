package org.fastnate.data;

import java.io.IOException;

import org.fastnate.generator.EntitySqlGenerator;

/**
 * Implementations of this class will automatically instantiated by the {@link EntityImporter}.
 *
 * The constructor must either be the "no arguments constructor" - or accept one or more of the following parameters:
 * <ul>
 * <li>the data directory as {@link java.io.File}</li>
 * <li>the {@link java.util.Properties} settings</li>
 * <li>references to other DataProviders</li>
 * </ul>
 *
 * If references to other DataProviders are used in the constructor, the {@link #buildEntities()} method of these
 * providers are called before our {@link #buildEntities()}.
 *
 * @author Andreas Penski
 * @author Tobias Liefke
 */
public interface DataProvider {

	/**
	 * Builds the entities that are accessed later using {@link #writeEntities(EntitySqlGenerator)}.
	 *
	 * @throws IOException
	 *             if something happens during any possible import of the generated entities
	 */
	void buildEntities() throws IOException;

	/**
	 * An additional helper to sort the output by its precedence.
	 *
	 * Providers with a smaller order criteria will write their data before providers with a higher order criteria,
	 * except in the case that the first is depending on the second.
	 *
	 * @return the order criteria - defaults to 0
	 */
	default int getOrder() {
		return 0;
	}

	/**
	 * Adds all {@link #buildEntities() entities} to the SQL file using the given generator.
	 *
	 * @param sqlGenerator
	 *            the SQL file generator
	 * @throws IOException
	 *             if the generator throws one
	 */
	void writeEntities(EntitySqlGenerator sqlGenerator) throws IOException;

}
