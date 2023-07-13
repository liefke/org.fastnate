package org.fastnate.data;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

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
public class InjectDataProviderFactory extends AbstractDataProviderFactory {

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

	/** The parent importer, set during {@link #createDataProviders(EntityImporter)}. */
	private EntityImporter importer;

	/** Contains the class path scanner with the data provider packages. */
	private Reflections reflections;

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

		// Now look for a public constructor
		final Constructor<C>[] constructors = (Constructor<C>[]) instanceClass.getConstructors();
		ModelException.test(constructors.length > 0, "No public constructor found for {}", instanceClass);

		// And find the one that is annotated with @Inject
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
		this.reflections = buildReflections(parentImporter);

		// Create instances
		for (final Class<? extends DataProvider> providerClass : findProviderClasses(this.reflections)) {
			if (!Modifier.isAbstract(providerClass.getModifiers()) && providerClass.getConstructors().length > 0) {
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
			injectSetters(instanceClass, new HashSet<>(), instance, injection);

			// Call any post initialization method
			invokePostInitialize(instanceClass, new HashSet<>(), instance);

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
			params[i] = findDependency(method, parameterTypes[i], injection);
		}
		return params;
	}

	private <C> Object findDependency(final Member member, final AnnotatedType annotatedType,
			final Injection parentInjection) {
		// Check for any qualifier annoation and build the lookup key
		final Class<C> type = (Class<C>) annotatedType.getType();
		final Annotation qualifier = findQualifier(annotatedType);
		final Dependency dependency = new Dependency(qualifier, type);
		Injection injection = this.injections.get(dependency);
		if (injection == null) {
			final C importerParamer = findImporterDependency(this.importer, type);
			if (importerParamer != null) {
				injection = new Injection(Integer.MIN_VALUE, importerParamer);
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
							"Could not find subclass of {} with qualifier {} when initializing {}", type, qualifier,
							member);
					ModelException.test(possibleImplementations.size() == 1,
							"More than one possible subclass for {} with qualifier {} found when initializing {}", type,
							qualifier, member);
					injection = buildInjection(possibleImplementations.get(0));
				} else {
					throw new ModelException("More than one matching subclasses of " + type
							+ " found when initializing " + member + ", use a qualifier for disambiguation");
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
						field.set(instance, findDependency(field, field.getAnnotatedType(), parentInjection));
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
