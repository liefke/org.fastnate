package org.fastnate.data;

/**
 * Finds and builds all implementations of {@link DataProvider} for the current environment.
 *
 * @author Tobias Liefke
 */
public interface DataProviderFactory {

	/**
	 * Discovers and builds all {@link DataProvider}s that are available for the current environment.
	 *
	 * @param importer
	 *            the current importer that needs the providers, add them with
	 */
	void createDataProviders(EntityImporter importer);

}
