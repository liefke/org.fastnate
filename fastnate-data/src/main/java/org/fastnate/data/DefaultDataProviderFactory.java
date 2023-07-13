package org.fastnate.data;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import org.fastnate.generator.context.ModelException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Finds and builds all implementations of {@link DataProvider} from the class path.
 *
 * Each found non abstract class extending {@link DataProvider} is instantiated:
 *
 * <ol>
 * <li>First a constructor with parameters of an allowed type is searched.</li>
 * <li>Then all fields and setters marked with {@link Resource @Resource} are initiated.</li>
 * <li>At the end all methods marked with {@link PostConstruct @PostConstruct} are invoked.</li>
 * <li>If nothing fails, the new provider is {@link EntityImporter#addDataProvider(DataProvider) registered}.</li>
 * </ol>
 *
 * Allowed types for constructor parameters and resources:
 * <ul>
 * <li>{@link EntityImporter} - the importer itself</li>
 * <li>{@link File} - the data directory from the command line resp. from the property
 * {@link EntityImporter#DATA_FOLDER_KEY}.</li>
 * <li>{@link Properties} - the {@link EntityImporter#getSettings() settings} of the {@link EntityImporter}</li>
 * <li>{@link DataProvider} - another provider, will throw an exception, if the given provider depends on the new
 * one</li>
 * </ul>
 *
 * The factory will search for the available data providers in the packages defined in the property
 * {@link EntityImporter#PACKAGES_KEY}. In addition one can define one or more data providers in the file
 * {@code /META-INF/services/org.fastnate.data.DataProvider}.
 *
 * @author Tobias Liefke
 */
public class DefaultDataProviderFactory extends AbstractDataProviderFactory {

	@Getter
	@Setter
	@NoArgsConstructor
	private static final class MaxOrder {

		private int value = Integer.MIN_VALUE;

		/**
		 * Adds the order of the given provider to this order agglomeration.
		 *
		 * @param provider
		 *            the dependency
		 */
		public void add(final DataProvider provider) {
			if (provider.getOrder() > this.value) {
				this.value = provider.getOrder();
			}
		}

	}

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

		Arrays.sort(constructors, Comparator.<Constructor<C>> comparingInt(c -> c.getParameterTypes().length)
				.thenComparing(Constructor::toGenericString));

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
		final MaxOrder maxOrder = new MaxOrder();

		// Fill all parameters
		final Class<?>[] parameterTypes = constructor.getParameterTypes();
		final Object[] params = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			final Object parameter = findDependency(importer, parameterTypes[i], maxOrder);
			if (parameter == null) {
				// This is not the correct constructor (at least up to now)
				return null;
			}
			params[i] = parameter;
		}

		// Find all resource injections
		final List<Consumer<DataProvider>> resources = new ArrayList<>();
		if (!findResourceFields(importer, constructor.getDeclaringClass(), resources, maxOrder)
				|| !findResourceSetters(importer, constructor.getDeclaringClass(), resources, new HashSet<>(),
						maxOrder)) {
			// There is still some resource missing
			return null;
		}

		try {
			// Create the provider
			final C provider = constructor.newInstance(params);
			resources.forEach(resource -> resource.accept(provider));
			invokePostConstruct(constructor.getDeclaringClass(), new HashSet<>(), provider);

			// And add it after the first provider with the same or a smaller order criteria
			importer.addDataProvider(provider, maxOrder.getValue());
			return provider;
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void createDataProviders(final EntityImporter importer) {
		// Find providers
		final List<Class<? extends DataProvider>> providerClasses = findProviderClasses(buildReflections(importer));

		// Create instances, depending on other needed providers
		while (!providerClasses.isEmpty()) {
			final int previousSize = providerClasses.size();

			for (final Iterator<Class<? extends DataProvider>> iterator = providerClasses.iterator(); iterator
					.hasNext();) {
				final Class<? extends DataProvider> providerClass = iterator.next();
				if (Modifier.isAbstract(providerClass.getModifiers()) || providerClass.getConstructors().length == 0
						|| addProvider(importer, providerClass) != null) {
					iterator.remove();
				}
			}

			// Prevent endless loops
			ModelException.test(providerClasses.size() < previousSize,
					"Can't instantiate the following providers (possibly because of circular or missing dependencies): {}",
					providerClasses);
		}
	}

	private <E> E findDependency(final EntityImporter importer, final Class<E> parameterType, final MaxOrder maxOrder) {
		final E dependency = findImporterDependency(importer, parameterType);
		if (dependency != null) {
			return dependency;
		}
		if (DataProvider.class.isAssignableFrom(parameterType)) {
			final DataProvider provider = importer.findDataProvider((Class<? extends DataProvider>) parameterType);
			if (provider != null) {
				maxOrder.add(provider);
				return (E) provider;
			}
		}
		// This is not a supported dependency
		return null;
	}

	private boolean findResourceFields(final EntityImporter importer, final Class<?> instanceClass,
			final List<Consumer<DataProvider>> resources, final MaxOrder maxOrder) {
		if (instanceClass != Object.class) {
			if (!findResourceFields(importer, instanceClass.getSuperclass(), resources, maxOrder)) {
				return false;
			}

			for (final Field field : instanceClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(Resource.class)) {
					final Object parameter = findDependency(importer, field.getType(), maxOrder);
					if (parameter == null) {
						return false;
					}
					resources.add(provider -> {
						field.setAccessible(true);
						try {
							field.set(provider, parameter);
						} catch (final IllegalAccessException e) {
							// Can't happen, as we set the field accessible
							throw new IllegalStateException(e);
						}
					});
				}
			}
		}
		return true;
	}

	private boolean findResourceSetters(final EntityImporter importer, final Class<?> instanceClass,
			final List<Consumer<DataProvider>> resources, final Set<String> injectedMethods, final MaxOrder maxOrder) {
		if (instanceClass != Object.class) {
			if (!findResourceSetters(importer, instanceClass.getSuperclass(), resources, injectedMethods, maxOrder)) {
				return false;
			}

			for (final Method method : instanceClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Resource.class)) {
					final Class<?>[] parameterTypes = method.getParameterTypes();
					if (parameterTypes.length == 1
							&& (Modifier.isPrivate(method.getModifiers()) || injectedMethods.add(method.getName()))) {
						final Object parameter = findDependency(importer, parameterTypes[0], maxOrder);
						if (parameter == null) {
							return false;
						}
						resources.add(provider -> {
							method.setAccessible(true);
							try {
								method.invoke(provider, parameter);
							} catch (final IllegalAccessException | InvocationTargetException e) {
								throw new IllegalStateException(e);
							}
						});
					}
				}
			}
		}
		return true;
	}

	private void invokePostConstruct(final Class<?> instanceClass, final Set<String> calledMethods,
			final DataProvider provider) {
		if (instanceClass != Object.class) {
			invokePostConstruct(instanceClass.getSuperclass(), calledMethods, provider);

			for (final Method method : instanceClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(PostConstruct.class)
						&& (Modifier.isPrivate(method.getModifiers()) || calledMethods.add(method.getName()))) {
					method.setAccessible(true);
					try {
						method.invoke(provider);
					} catch (final IllegalAccessException | InvocationTargetException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

}
