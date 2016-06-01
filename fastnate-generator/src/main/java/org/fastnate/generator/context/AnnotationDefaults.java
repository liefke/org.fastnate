package org.fastnate.generator.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Creates an implementation of an annotation class to access the default values for the single values.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class AnnotationDefaults implements InvocationHandler {

	/**
	 * Creates a proxy class that returns the default value from the annotation for every method.
	 *
	 * @param annotation
	 *            the annotation class
	 * @return the proxy class containing the default values
	 */
	public static <A extends Annotation> A create(final Class<A> annotation) {
		return create(annotation, Collections.emptyMap());
	}

	/**
	 * Creates a proxy class that returns the default value from the annotation for every method.
	 *
	 * @param annotation
	 *            the annotation class
	 * @param overrides
	 *            the name methods that should return a value different from default
	 * @return the proxy class containing the default values
	 */
	public static <A extends Annotation> A create(final Class<A> annotation, final Map<String, Object> overrides) {
		return (A) Proxy.newProxyInstance(annotation.getClassLoader(), new Class[] { annotation },
				new AnnotationDefaults(overrides));
	}

	private final Map<String, Object> overrides;

	/**
	 * Finds the default value for the original method.
	 */
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) {
		final Object value = this.overrides.get(method.getName());
		return value != null ? value : method.getDefaultValue();
	}
}
