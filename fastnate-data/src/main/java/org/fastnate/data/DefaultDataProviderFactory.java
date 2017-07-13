package org.fastnate.data;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.fastnate.generator.context.ModelException;
import org.reflections.Reflections;

/**
 * Finds and builds all implementations of {@link DataProvider} from the class path.
 *
 * @author Tobias Liefke
 */
public class DefaultDataProviderFactory implements DataProviderFactory {

	/**
	 * Tries to instantiate the provider from the given class.
	 *
	 * @param importer
	 *            the current importer that will be used with that provider
	 * @param providerClass
	 *            the class of the provider to instantiate
	 * @return the created provider or {@code null} if the given class has no appropriate constructor
	 */
	protected <C extends DataProvider> C addProvider(final EntityImporter importer, final Class<C> providerClass) {
		final Constructor<C>[] constructors = (Constructor<C>[]) providerClass.getConstructors();
		ModelException.test(constructors.length > 0, "No public constructor found for {}", providerClass);

		for (final Constructor<C> constructor : constructors) {
			final C provider = addProvider(importer, constructor);
			if (provider != null) {
				return provider;
			}
		}
		return null;
	}

	/**
	 * Tries to create a provider using the given constructor and adds it to list of providers in the
	 * {@link EntityImporter}.
	 *
	 * @param importer
	 *            the current instance of the importer for adding the new provider
	 *
	 * @param constructor
	 *            the constructor of the new provider
	 * @return the created provider or {@code null} if the given constructor is not appropriate
	 */
	protected <C extends DataProvider> C addProvider(final EntityImporter importer, final Constructor<C> constructor) {
		// Remember the maximum order criteria of the parameters
		int maxOrder = Integer.MIN_VALUE;

		// Fill all parameters
		final Class<?>[] parameterTypes = constructor.getParameterTypes();
		final Object[] params = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			final Class<?> parameterType = parameterTypes[i];
			if (parameterType == File.class) {
				params[i] = importer.getDataFolder();
			} else if (parameterType == Properties.class) {
				params[i] = importer.getSettings();
			} else if (DataProvider.class.isAssignableFrom(parameterType)) {
				final DataProvider parameter = importer.findDataProvider((Class<? extends DataProvider>) parameterType);
				if (parameter == null) {
					// No matching data provider found -> this is not our constructor (at least up to now)
					return null;
				}
				params[i] = parameter;
				maxOrder = Math.max(maxOrder, parameter.getOrder());
			} else {
				// This is not a constructor with a supported parameter type
				return null;
			}
		}
		try {
			// Create the provider
			final C provider = constructor.newInstance(params);

			// And add it after the first provider with the same or a smaller order criteria
			importer.addDataProvider(provider, maxOrder);
			return provider;
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void createDataProviders(final EntityImporter importer) {
		// Find providers
		final List<Class<? extends DataProvider>> providerClasses = findProviderClasses(importer);

		// Create instances, depending on other needed providers
		while (!providerClasses.isEmpty()) {
			final int previousSize = providerClasses.size();

			for (final Iterator<Class<? extends DataProvider>> iterator = providerClasses.iterator(); iterator
					.hasNext();) {
				final Class<? extends DataProvider> providerClass = iterator.next();
				if (Modifier.isAbstract(providerClass.getModifiers()) || addProvider(importer, providerClass) != null) {
					iterator.remove();
				}
			}

			// Prevent endless loops
			ModelException.test(providerClasses.size() < previousSize,
					"Can't instantiate the following providers (possibly because of circular dependencies): {}",
					providerClasses);
		}
	}

	/**
	 * Finds all provider classes from the class path, that implement {@link DataProvider}.
	 *
	 * @param importer
	 *            the current importer that contains the optional settings
	 * @return all found classes
	 */
	protected List<Class<? extends DataProvider>> findProviderClasses(final EntityImporter importer) {
		final String packages = EntityImporter.class.getPackage().getName() + ";"
				+ importer.getSettings().getProperty(EntityImporter.PACKAGES_KEY, "").trim();
		final Reflections reflections = new Reflections((Object[]) packages.split("[\\s;,:]+"));
		final List<Class<? extends DataProvider>> providerClasses = new ArrayList<>(
				reflections.getSubTypesOf(DataProvider.class));

		// Use a fixed order to ensure always the same order of instantiation
		Collections.sort(providerClasses, (c1, c2) -> c1.getName().compareTo(c2.getName()));
		return providerClasses;
	}

}
