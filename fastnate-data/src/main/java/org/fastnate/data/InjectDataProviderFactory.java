package org.fastnate.data;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.fastnate.generator.context.ModelException;
import org.reflections.Reflections;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Find and initializes {@link DataProvider}s on the classpath. Dependencies between DataProviders are {@link Inject
 * injected}.
 *
 * @author Tobias Liefke
 */
public class InjectDataProviderFactory implements DataProviderFactory {

	@EqualsAndHashCode
	@RequiredArgsConstructor
	private static final class Dependency {

		private final Annotation qualifier;

		private final Class<?> type;

		@Override
		public String toString() {
			return this.qualifier == null ? this.type.toString() : this.qualifier + " " + this.type;
		}

	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	private static final class Injection {

		private int order = Integer.MIN_VALUE;

		private Object instance;

		@Override
		public String toString() {
			return this.instance == null ? "" : this.instance.toString();
		}

	}

	private static Annotation findQualifier(final AnnotatedElement annotatedElement) {
		return Arrays.stream(annotatedElement.getAnnotations())
				.filter(a -> a.getClass().isAnnotationPresent(Qualifier.class)).findFirst().orElse(null);
	}

	private final Map<Dependency, Injection> injections = new HashMap<>();

	private Reflections reflections;

	private EntityImporter importer;

	private <C> Injection buildInjection(final Class<C> instanceClass) {
		ModelException.test(!Modifier.isAbstract(instanceClass.getModifiers()),
				"Can't instantiate instance of abstract {}", instanceClass);

		// First check, if we are already added (or adding ourselfes)
		final Dependency dependency = new Dependency(findQualifier(instanceClass), instanceClass);
		Injection injection = this.injections.get(dependency);
		if (injection != null) {
			ModelException.test(injection.getInstance() != null, "Cicular dependency in {}", instanceClass);
			return injection;
		}

		// And remember that we started to add this instance
		injection = new Injection();
		this.injections.put(dependency, injection);

		// Now look for a constructor annotated with @Inject
		final Constructor<C>[] constructors = (Constructor<C>[]) instanceClass.getDeclaredConstructors();
		ModelException.test(constructors.length > 0, "No constructor found for {}", instanceClass);

		Constructor<C> potentialConstructor = null;
		for (final Constructor<C> constructor : constructors) {
			if (constructor.isAnnotationPresent(Inject.class)) {
				return fillInjection(injection, constructor);
			}

			if (constructor.getParameterTypes().length == 0 || potentialConstructor == null) {
				potentialConstructor = constructor;
			}
		}

		ModelException.test(potentialConstructor != null, "No constructor found for {}", instanceClass);

		// If no @Inject is found, use the the first available constructor
		return fillInjection(injection, potentialConstructor);
	}

	@Override
	public void createDataProviders(final EntityImporter parentImporter) {
		this.importer = parentImporter;

		// Instantiate reflections for discovering possible injections
		final String packages = EntityImporter.class.getPackage().getName() + ";"
				+ this.importer.getSettings().getProperty(EntityImporter.PACKAGES_KEY, "").trim();
		this.reflections = new Reflections((Object[]) packages.split("[\\s;,:]+"));

		// Find all provider classes
		final List<Class<? extends DataProvider>> providerClasses = new ArrayList<>(
				this.reflections.getSubTypesOf(DataProvider.class));

		// Use ServiceLoader to find all providers defined in /META-INF/services/org.fastnate.data.DataProvider
		final ServiceLoader<? extends DataProvider> serviceLoader = ServiceLoader.load(DataProvider.class);
		serviceLoader.forEach(provider -> providerClasses.add(provider.getClass()));

		// Use a fixed order to ensure always the same order of instantiation
		Collections.sort(providerClasses, Comparator.comparing(Class::getName));

		// Create instances
		for (final Class<? extends DataProvider> providerClass : providerClasses) {
			if (!Modifier.isAbstract(providerClass.getModifiers())) {
				buildInjection(providerClass);
			}
		}
	}

	private <C> Injection fillInjection(final Injection injection, final Constructor<C> constructor) {
		try {
			// Create the instance
			constructor.setAccessible(true);
			final C instance = constructor.newInstance(fillParameters(constructor, injection));

			// Fill all fields
			final Class<C> instanceClass = constructor.getDeclaringClass();
			injectFields(instanceClass, instance, injection);

			// Call all setter
			final HashSet<String> calledMethods = new HashSet<>();
			injectSetters(instanceClass, calledMethods, instance, injection);

			// Call any post initialization method
			invokePostInitialize(instanceClass, calledMethods, instance);

			injection.setInstance(instance);

			if (instance instanceof DataProvider) {
				final DataProvider provider = (DataProvider) instance;
				if (provider.getOrder() > injection.getOrder()) {
					injection.setOrder(provider.getOrder());
				}
				this.importer.addDataProvider(provider, injection.getOrder());
			}
			return injection;
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private Object[] fillParameters(final Executable method, final Injection injection) {
		final AnnotatedType[] parameterTypes = method.getAnnotatedParameterTypes();
		final Object[] params = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			params[i] = findDependency(parameterTypes[i], injection);
		}
		return params;
	}

	private <C> Object findDependency(final AnnotatedType annotatedType, final Injection parentInjection) {
		// Check for any qualifier annoation and build the lookup key
		final Class<C> type = (Class<C>) annotatedType.getType();
		final Annotation qualifier = findQualifier(annotatedType);
		final Dependency dependency = new Dependency(qualifier, type);
		Injection injection = this.injections.get(dependency);
		if (injection == null) {
			if (type == EntityImporter.class) {
				injection = new Injection(Integer.MIN_VALUE, this.importer);
			} else if (type == File.class) {
				injection = new Injection(Integer.MIN_VALUE, this.importer.getDataFolder());
			} else if (type == Properties.class) {
				injection = new Injection(Integer.MIN_VALUE, this.importer.getSettings());
			} else {
				// Try to find the correct instance
				final List<Class<? extends C>> possibleImplementations = this.reflections.getSubTypesOf(type).stream()
						.filter(c -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toList());
				if (possibleImplementations.isEmpty()) {
					// No instance in the scanned path, try to instantiate the class itself
					injection = buildInjection(type);
				} else if (possibleImplementations.size() == 1) {
					// No need for an qualifier - use the only possible instance
					injection = buildInjection(possibleImplementations.get(0));
				} else if (qualifier != null) {
					// Find the correct instance
					possibleImplementations.removeIf(
							c -> !Arrays.asList(c.getAnnotationsByType(qualifier.getClass())).contains(qualifier));
					ModelException.test(!possibleImplementations.isEmpty(),
							"Could not find subclass of {} with qualifier {}", type, qualifier);
					ModelException.test(possibleImplementations.size() == 1,
							"More than one possible subclass for {} with qualifier {} found", type, qualifier);
					injection = buildInjection(possibleImplementations.get(0));
				} else {
					throw new ModelException("More than one matching subclasses of " + type
							+ " found, use a qualifier for disambiguation");
				}
			}
			this.injections.put(dependency, injection);
		}
		final int order = injection.getOrder();
		if (parentInjection.getOrder() < order) {
			parentInjection.setOrder(order);
		}
		return injection.getInstance();
	}

	private void injectFields(final Class<?> instanceClass, final Object instance, final Injection parentInjection) {
		if (instanceClass != Object.class) {
			injectFields(instanceClass.getSuperclass(), instance, parentInjection);

			for (final Field field : instanceClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(Resource.class)) {
					field.setAccessible(true);
					try {
						field.set(instance, findDependency(field.getAnnotatedType(), parentInjection));
					} catch (final IllegalAccessException e) {
						// Can't happen, as we set the field accessible
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	private void injectSetters(final Class<?> instanceClass, final Set<String> injectedMethods, final Object instance,
			final Injection parentInjection) {
		if (instanceClass != Object.class) {
			injectSetters(instanceClass.getSuperclass(), injectedMethods, instance, parentInjection);

			for (final Method method : instanceClass.getDeclaredMethods()) {
				if ((method.isAnnotationPresent(Inject.class) || method.isAnnotationPresent(Resource.class))
						&& (Modifier.isPrivate(method.getModifiers())
								|| injectedMethods.add(method.toGenericString()))) {
					try {
						method.invoke(instance, fillParameters(method, parentInjection));
					} catch (final IllegalAccessException | InvocationTargetException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	private void invokePostInitialize(final Class<?> instanceClass, final Set<String> calledMethods,
			final Object instance) {
		if (instanceClass != Object.class) {
			invokePostInitialize(instanceClass.getSuperclass(), calledMethods, instance);

			for (final Method method : instanceClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(PostConstruct.class)
						&& (Modifier.isPrivate(method.getModifiers()) || calledMethods.add(method.getName()))) {
					method.setAccessible(true);
					try {
						method.invoke(instance);
					} catch (final IllegalAccessException | InvocationTargetException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}
}
