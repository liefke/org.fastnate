package org.fastnate.generator.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Creates an implementation of an annotation class to access the default values for the single values.
 *
 * @author Tobias Liefke
 */
public class AnnotationDefaults implements InvocationHandler {

	/**
	 * Creates a proxy class that returns the default value from the annotation for every method.
	 *
	 * @param annotation
	 *            the annotation class
	 * @return the proxy class containing the default values
	 */
	public static <A extends Annotation> A create(final Class<A> annotation) {
		return (A) Proxy.newProxyInstance(annotation.getClassLoader(), new Class[] { annotation },
				new AnnotationDefaults());
	}

	/**
	 * Finds the default value for the original method.
	 */
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) {
		return method.getDefaultValue();
	}
}
