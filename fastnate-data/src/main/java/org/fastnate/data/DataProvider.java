package org.fastnate.data;

import java.io.IOException;
import java.util.Collection;

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
 * @author apenski
 * @author Tobias Liefke
 */
public interface DataProvider {

	/**
	 * Builds the entities that are accessed later using {@link #getEntities()}.
	 *
	 * @throws IOException
	 *             if something happens during any possible import of the generated entities
	 */
	void buildEntities() throws IOException;

	/**
	 * The list of entities for which INSERT SQL statements will be generated.
	 *
	 * @return list of entities
	 */
	Collection<?> getEntities();

}
