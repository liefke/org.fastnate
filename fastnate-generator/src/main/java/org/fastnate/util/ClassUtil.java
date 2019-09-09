package org.fastnate.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Helper for inspection of classes.
 *
 * @author Tobias Liefke
 */
public final class ClassUtil {

	/** Mapping from a number class to its mapping function. */
	private static final Map<Class<? extends Number>, Function<Number, Number>> NUMBER_MAPPERS = new HashMap<>();

	static {
		// Primitive and wrapper types
		NUMBER_MAPPERS.put(Byte.class, Number::byteValue);
		NUMBER_MAPPERS.put(byte.class, Number::byteValue);
		NUMBER_MAPPERS.put(Short.class, Number::shortValue);
		NUMBER_MAPPERS.put(short.class, Number::shortValue);
		NUMBER_MAPPERS.put(Integer.class, Number::intValue);
		NUMBER_MAPPERS.put(int.class, Number::intValue);
		NUMBER_MAPPERS.put(Long.class, Number::longValue);
		NUMBER_MAPPERS.put(long.class, Number::longValue);
		NUMBER_MAPPERS.put(Float.class, Number::floatValue);
		NUMBER_MAPPERS.put(float.class, Number::floatValue);
		NUMBER_MAPPERS.put(Double.class, Number::doubleValue);
		NUMBER_MAPPERS.put(double.class, Number::doubleValue);

		// Big types
		NUMBER_MAPPERS.put(BigInteger.class,
				n -> n instanceof BigDecimal ? ((BigDecimal) n).toBigInteger() : BigInteger.valueOf(n.longValue()));
		NUMBER_MAPPERS.put(BigDecimal.class,
				n -> n instanceof BigInteger ? new BigDecimal((BigInteger) n) : BigDecimal.valueOf(n.doubleValue()));

		// Atomic types
		NUMBER_MAPPERS.put(AtomicInteger.class, n -> new AtomicInteger(n.intValue()));
		NUMBER_MAPPERS.put(AtomicLong.class, n -> new AtomicLong(n.longValue()));
	}

	/**
	 * Tries to convert the given number to the given type.
	 *
	 * @param number
	 *            the number that we have
	 * @param targetType
	 *            the type that we need
	 * @return the number in the given type
	 *
	 * @throws IllegalArgumentException
	 *             if the target type is not a common number type
	 */
	public static <N extends Number> N convertNumber(final Number number, final Class<N> targetType) {
		if (targetType.isInstance(number) || number == null) {
			return (N) number;
		}
		final Function<Number, N> mapper = (Function<Number, N>) NUMBER_MAPPERS.get(targetType);
		if (mapper == null) {
			throw new IllegalArgumentException("Unknown number type: " + targetType);
		}
		return mapper.apply(number);
	}

	private static <I> Type getActualTypeArgument(final Class<? extends I> instanceClass, final Class<I> superClass,
			final int argumentIndex) {
		if (instanceClass == superClass) {
			return instanceClass.getTypeParameters()[argumentIndex];
		}

		final List<Type> parents = new ArrayList<>();
		parents.add(instanceClass.getGenericSuperclass());
		parents.addAll(Arrays.asList(instanceClass.getGenericInterfaces()));
		for (final Type parentType : parents) {
			final Class<?> parentClass = parentType instanceof ParameterizedType
					? (Class<?>) ((ParameterizedType) parentType).getRawType()
					: (Class<?>) parentType;

			// First check if we found the super class or interface
			if (superClass.equals(parentClass)) {
				// We found the requested super class - use the binding
				return ((ParameterizedType) parentType).getActualTypeArguments()[argumentIndex];
			} else if (parentClass != null && superClass.isAssignableFrom(parentClass)) {
				// Else step up
				final Type type = getActualTypeArgument(parentClass.asSubclass(superClass), superClass, argumentIndex);
				if (type instanceof Class) {
					return type;
				} else if (type instanceof TypeVariable) {
					return ((ParameterizedType) parentType).getActualTypeArguments()[Arrays
							.asList(parentClass.getTypeParameters()).indexOf(type)];
				}
			}
		}

		return null;
	}

	/**
	 * Resolves the actual binding of a generic type that was specified in a superclass and bound in a subclass.
	 *
	 * @param instanceClass
	 *            the implementing class
	 * @param superClass
	 *            the superclass or interface which specifies the generic type variable
	 * @param argumentIndex
	 *            the index of the type variable in the superclass definition (0 = the first type variable)
	 * @return the bound class for the variable in instanceClass
	 */
	public static <T, I> Class<T> getActualTypeBinding(final Class<? extends I> instanceClass,
			final Class<I> superClass, final int argumentIndex) {
		Type type = getActualTypeArgument(instanceClass, superClass, argumentIndex);
		while (!(type instanceof Class<?>)) {
			if (type instanceof WildcardType) {
				type = ((WildcardType) type).getUpperBounds()[0];
			} else if (type instanceof TypeVariable<?>) {
				type = ((TypeVariable<?>) type).getBounds()[0];
			} else if (type instanceof ParameterizedType) {
				type = ((ParameterizedType) type).getRawType();
			} else {
				throw new NoSuchElementException("Can't find binding for the " + argumentIndex + ". argument of "
						+ superClass + " in " + instanceClass);
			}
		}
		return (Class<T>) type;
	}

	/**
	 * Resolves the actual binding of a generic type to a specific class by inspecting type variables in the declaring
	 * class and the (sub)class of the object.
	 *
	 * @param instanceClass
	 *            the class that implements the object and may contain the binding of the type parameters
	 * @param declaringClass
	 *            the class that declared the attribute
	 * @param attributeType
	 *            the return type, parameter type or field type of the attribute that may contain references to type
	 *            parameters
	 * @return the qualified type of the attribute according to the instance class
	 */
	public static <T, I> Class<T> getActualTypeBinding(final Class<? extends I> instanceClass,
			final Class<I> declaringClass, final Type attributeType) {
		if (attributeType instanceof Class<?>) {
			return (Class<T>) attributeType;
		}
		if (attributeType instanceof TypeVariable<?>) {
			final String name = ((TypeVariable<?>) attributeType).getName();
			final TypeVariable<?>[] typeParameters = declaringClass.getTypeParameters();
			for (int i = 0; i < typeParameters.length; i++) {
				final TypeVariable<?> variable = typeParameters[i];
				if (variable.getName().equals(name)) {
					return getActualTypeBinding(instanceClass, declaringClass, i);
				}
			}
		}
		if (attributeType instanceof WildcardType) {
			final Type bound = ((WildcardType) attributeType).getUpperBounds()[0];
			if (bound != attributeType) {
				return getActualTypeBinding(instanceClass, declaringClass, bound);
			}
		}
		if (attributeType instanceof ParameterizedType) {
			final Type rawType = ((ParameterizedType) attributeType).getRawType();
			if (!rawType.equals(attributeType)) {
				return getActualTypeBinding(instanceClass, declaringClass, rawType);
			}
		}

		throw new NoSuchElementException("Can't find binding for " + attributeType + " declared in " + declaringClass
				+ " and implented in " + instanceClass);
	}

	/**
	 * Resolves the name of the method that called the caller of this method.
	 *
	 * @return the name of the caller
	 */
	public static String getCallerMethod() {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		for (int i = 2; i < stackTrace.length; i++) {
			final String methodName = stackTrace[i].getMethodName();
			if (methodName.indexOf('$') < 0) {
				return methodName;
			}
		}
		return stackTrace[1].getMethodName();
	}

	/**
	 * Resolves the name of the method that called the caller of this method.
	 *
	 * Steps up until the caller belongs to the given class
	 *
	 * @param callerClass
	 *            the class that the method belongs to
	 *
	 * @return the name of the caller
	 */
	public static String getCallerMethod(final Class<?> callerClass) {
		final String callerClassName = callerClass.getName();
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		for (int i = 2; i < stackTrace.length; i++) {
			final String methodName = stackTrace[i].getMethodName();
			if (methodName.indexOf('$') < 0 && callerClassName.equals(stackTrace[i].getClassName())) {
				return methodName;
			}
		}
		return stackTrace[1].getMethodName();
	}

	private ClassUtil() {
		// Helper class
	}

}
