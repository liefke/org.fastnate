package org.fastnate.data;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;

import org.fastnate.data.files.DataFolder;
import org.fastnate.generator.context.GeneratorContext;
import org.reflections.Reflections;

/**
 * Base classes for implementations of {@link DataProviderFactory}.
 *
 * @author Tobias Liefke
 */
public abstract class AbstractDataProviderFactory implements DataProviderFactory {

	/**
	 * Builds the reflections object for scanning for data providers.
	 *
	 * @param importer
	 *            the importer that contains the settings
	 * @return the class path scanner for the data provider packages
	 */
	protected Reflections buildReflections(final EntityImporter importer) {
		final String packages = EntityImporter.class.getPackage().getName() + ";"
				+ importer.getSettings().getProperty(EntityImporter.PACKAGES_KEY, "").trim();

		final Reflections reflections = new Reflections((Object[]) packages.split("[\\s;,:]+"));

		// Scan for entity classes
		reflections.getTypesAnnotatedWith(Entity.class).forEach(importer.getContext()::getDescription);

		return reflections;
	}

	/**
	 * Models dependencies from the importer.
	 *
	 * @param importer
	 *            the current importer
	 * @param dependencyClass
	 *            the class of the property to inject into a new {@link DataProvider}
	 * @return the dependency or {@code null} if not found
	 */
	protected <E> E findImporterDependency(final EntityImporter importer, final Class<E> dependencyClass) {
		if (dependencyClass == EntityImporter.class) {
			return (E) importer;
		}
		if (dependencyClass == GeneratorContext.class) {
			return (E) importer.getContext();
		}
		if (dependencyClass == DataFolder.class) {
			return (E) importer.getDataFolder();
		}
		if (dependencyClass == Properties.class) {
			return (E) importer.getSettings();
		}
		return null;
	}

	/**
	 * Finds all provider classes from the class path, that implement {@link DataProvider}.
	 *
	 * @param reflections
	 *            contains all the packages, that should be scanned
	 * @return all found classes
	 */
	protected List<Class<? extends DataProvider>> findProviderClasses(final Reflections reflections) {
		final Set<Class<? extends DataProvider>> providerClasses = reflections.getSubTypesOf(DataProvider.class);

		// Use ServiceLoader to find all providers defined in /META-INF/services/org.fastnate.data.DataProvider
		final ServiceLoader<? extends DataProvider> serviceLoader = ServiceLoader.load(DataProvider.class);
		serviceLoader.forEach(provider -> providerClasses.add(provider.getClass()));

		// Use a fixed order to ensure always the same order of instantiation
		return providerClasses.stream().sorted(Comparator.comparing(Class::getName)).collect(Collectors.toList());
	}

}
