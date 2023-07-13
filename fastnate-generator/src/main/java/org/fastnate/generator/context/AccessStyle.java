package org.fastnate.generator.context;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.util.ClassUtil;
import org.hibernate.annotations.Formula;

import lombok.Getter;

/**
 * Indicates how persistent attributes are accessed.
 *
 * @author Tobias Liefke
 */
public enum AccessStyle {
	/** The persistent attributes are read using field access. */
	FIELD {

		final class Accessor implements AttributeAccessor {

			/** The sub class that may contain any generic type parameter. */
			@Getter
			private final Class<?> implementationClass;

			/** The field of the attribute. */
			@Getter
			private final Field field;

			/** The name of the attribute, inherited from {@link #field}. */
			@Getter
			private final String name;

			/**
			 * The return type of the method. If the return type is generic, the type is resolved with the generic type
			 * parameters of the {@link #implementationClass}.
			 */
			@Getter
			private final Class<?> type;

			/**
			 * Creates a new instance of a attribute with field access.
			 *
			 * @param implementationClass
			 *            The sub class that may contain any generic type parameter.
			 * @param field
			 *            the field of the attribute
			 */
			Accessor(final Class<?> implementationClass, final Field field) {
				this.implementationClass = implementationClass;
				this.field = field;
				this.name = field.getName();
				this.type = ClassUtil.getActualTypeBinding(implementationClass,
						(Class<Object>) field.getDeclaringClass(), field.getGenericType());
			}

			@Override
			public AccessStyle getAccessStyle() {
				return FIELD;
			}

			@Override
			public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
				return this.field.getAnnotation(annotationClass);
			}

			@Override
			public Annotation[] getAnnotations() {
				return this.field.getAnnotations();
			}

			@Override
			public Annotation[] getDeclaredAnnotations() {
				return this.field.getDeclaredAnnotations();
			}

			@Override
			public Class<?> getDeclaringClass() {
				return this.field.getDeclaringClass();
			}

			@Override
			public AnnotatedElement getElement() {
				return this.field;
			}

			@Override
			public Type getGenericType() {
				return this.field.getGenericType();
			}

			@Override
			public <E, T> T getValue(final E entity) {
				if (entity == null) {
					return null;
				}
				try {
					if (!this.field.isAccessible()) {
						this.field.setAccessible(true);
					}
					return (T) this.field.get(entity);
				} catch (final ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			public boolean isPersistent() {
				final int modifiers = this.field.getModifiers();
				return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)
						&& !isAnnotationPresent(Transient.class) && !isAnnotationPresent(Formula.class);
			}

			@Override
			public <E, T> void setValue(final E entity, final T value) {
				try {
					if (!this.field.isAccessible()) {
						this.field.setAccessible(true);
					}
					this.field.set(entity, value);
				} catch (final ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			public String toString() {
				return this.field.toString();
			}

		}

		@Override
		public <E> Iterable<AttributeAccessor> getDeclaredAttributes(final Class<E> inspectedClass,
				final Class<? extends E> implementationClass) {
			final List<AttributeAccessor> result = new ArrayList<>();
			for (final Field field : inspectedClass.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
					result.add(new Accessor(implementationClass, field));
				}
			}
			return result;
		}

	},
	/** The persistent attributes are read using method access. */
	METHOD {

		final class Accessor implements AttributeAccessor {

			/** The sub class that may contain any generic type parameter. */
			@Getter
			private final Class<?> implementationClass;

			/** The getter of the property. */
			private final Method method;

			/** The setter of the property. */
			private Method setter;

			/** The name of the attribute, inherited from {@link #method}. */
			@Getter
			private final String name;

			/**
			 * The return type of the method. If the return type is generic, the type is resolved with the generic type
			 * parameters of the {@link #implementationClass}.
			 */
			@Getter
			private final Class<?> type;

			/**
			 * Creates a new instance with property style access.
			 *
			 * @param implementationClass
			 *            The sub class that may contain any generic type parameter.
			 * @param getter
			 *            the method to read the property
			 */
			Accessor(final Class<?> implementationClass, final Method getter) {
				this.implementationClass = implementationClass;
				this.method = getter;
				this.name = Introspector.decapitalize(getter.getName().replaceFirst("^(get|is)", ""));
				this.type = ClassUtil.getActualTypeBinding(implementationClass,
						(Class<Object>) getter.getDeclaringClass(), getter.getGenericReturnType());
			}

			@Override
			public AccessStyle getAccessStyle() {
				return METHOD;
			}

			@Override
			public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
				return this.method.getAnnotation(annotationClass);
			}

			@Override
			public Annotation[] getAnnotations() {
				return this.method.getAnnotations();
			}

			@Override
			public Annotation[] getDeclaredAnnotations() {
				return this.method.getDeclaredAnnotations();
			}

			@Override
			public Class<?> getDeclaringClass() {
				return this.method.getDeclaringClass();
			}

			@Override
			public AnnotatedElement getElement() {
				return this.method;
			}

			@Override
			public Type getGenericType() {
				return this.method.getGenericReturnType();
			}

			@Override
			public <E, T> T getValue(final E entity) {
				if (entity == null) {
					return null;
				}
				try {
					if (!this.method.isAccessible()) {
						this.method.setAccessible(true);
					}
					return (T) this.method.invoke(entity);
				} catch (final ReflectiveOperationException | IllegalArgumentException e) {
					throw new IllegalStateException("Could not execute " + this.method + " on " + entity + ": " + e, e);
				}
			}

			@Override
			public boolean isPersistent() {
				final int modifiers = this.method.getModifiers();
				return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)
						&& !isAnnotationPresent(Transient.class) && !isAnnotationPresent(Formula.class);
			}

			@Override
			public <E, T> void setValue(final E entity, final T value) {
				try {
					if (this.setter == null) {
						final String setterName = "set" + StringUtils.capitalize(this.name);
						final Class<?> paramType = this.method.getReturnType();
						for (final Method m : this.method.getDeclaringClass().getDeclaredMethods()) {
							if (m.getName().equals(setterName)) {
								final Class<?>[] parameterTypes = m.getParameterTypes();
								if (parameterTypes.length == 1 && parameterTypes[0] == paramType) {
									this.setter = m;
									break;
								}
							}
						}
						if (this.setter == null) {
							throw new ModelException("Can't find setter: " + this.method.getDeclaringClass() + '.'
									+ setterName + '(' + paramType + ')');
						}

						if (!this.setter.isAccessible()) {
							this.setter.setAccessible(true);
						}
					}
					this.setter.invoke(entity, value);
				} catch (final ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			public String toString() {
				return this.method.toString();
			}

		}

		@Override
		public <E> Iterable<AttributeAccessor> getDeclaredAttributes(final Class<E> inspectedClass,
				final Class<? extends E> implementationClass) {
			final List<AttributeAccessor> result = new ArrayList<>();
			for (final Method method : inspectedClass.getDeclaredMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) && !method.isSynthetic()
						&& method.getParameterCount() == 0) {
					final String name = method.getName();
					if (name.startsWith("get") || name.startsWith("is")) {
						result.add(new Accessor(implementationClass, method));
					}
				}
			}
			return result;
		}

	};

	/**
	 * Determines the correct mapping to the style for the given accesstype.
	 *
	 * @param type
	 *            the access type as defined in an {@link Access} annotation.
	 * @return the matching style
	 */
	public static AccessStyle getStyle(final AccessType type) {
		return type == AccessType.FIELD ? FIELD : METHOD;
	}

	/**
	 * Finds all attributes of the current access type that are referenced in the given class.
	 *
	 * @param inspectedClass
	 *            the class to inspect
	 * @param implementationClass
	 *            the class that is actually the type of the object, either {@code inspectedClass} or a sub class -
	 *            necessary for resolving generics
	 * @return all non static attributes found in the given class
	 */
	public abstract <E> Iterable<AttributeAccessor> getDeclaredAttributes(Class<E> inspectedClass,
			Class<? extends E> implementationClass);

}