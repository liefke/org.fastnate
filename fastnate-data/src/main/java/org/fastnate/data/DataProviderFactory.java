package org.fastnate.data;

/**
 * Finds and builds all implementations of {@link DataProvider} for the current environment.
 *
 * @author Tobias Liefke
 */
public interface DataProviderFactory {

	/**
	 * Discovers, builds and registers all {@link DataProvider}s that are available for the current environment.
	 *
	 * Ensures that the order of the providers is set in accordance with their dependencies.
	 *
	 * @param importer
	 *            the current importer that needs the providers, new providers are added with
	 *            {@link EntityImporter#addDataProvider(DataProvider)} or
	 *            {@link EntityImporter#addDataProvider(DataProvider, int)}
	 */
	void createDataProviders(EntityImporter importer);

}
