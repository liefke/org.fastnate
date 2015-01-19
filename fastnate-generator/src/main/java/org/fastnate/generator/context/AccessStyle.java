package org.fastnate.generator.context;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Transient;

import lombok.Getter;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Formula;

/**
 * Indicates how persistent properties are accessed.
 *
 * @author Tobias Liefke
 */
public enum AccessStyle {
	/** The persistent properties are read using field access. */
	FIELD {

		final class Accessor implements PropertyAccessor {

			/** The field of the property. */
			@Getter
			private final Field field;

			/** The name of the property, inherited from {@link #field}. */
			@Getter
			private final String name;

			/**
			 * Creates a new instance of a property with field access.
			 *
			 * @param field
			 *            the field of the property
			 */
			Accessor(final Field field) {
				this.field = field;
				this.name = field.getName();
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
			public Type getGenericType() {
				return this.field.getGenericType();
			}

			@Override
			public Class<?> getType() {
				return this.field.getType();
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
			public boolean hasAnnotation(final Class<? extends Annotation> annotationClass) {
				return this.field.isAnnotationPresent(annotationClass);
			}

			@Override
			public boolean isPersistentProperty() {
				final int modifiers = this.field.getModifiers();
				return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)
						&& !hasAnnotation(Transient.class) && !hasAnnotation(Formula.class);
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

		}

		@Override
		public <E> Iterable<PropertyAccessor> getDeclaredProperties(final Class<E> inspectedClass) {
			final List<PropertyAccessor> result = new ArrayList<>();
			for (final Field field : inspectedClass.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					result.add(new Accessor(field));
				}
			}
			return result;
		}

	},
	/** The persistent properties are read using method access. */
	METHOD {

		final class Accessor implements PropertyAccessor {

			/** The getter of the property. */
			private final Method method;

			/** The setter of the property. */
			private Method setter;

			/** The name of the property, inherited from {@link #method}. */
			@Getter
			private final String name;

			/**
			 * Creates a new instance with property style access.
			 *
			 * @param getter
			 *            the method to read the property
			 */
			public Accessor(final Method getter) {
				this.method = getter;
				this.name = Introspector.decapitalize(getter.getName().replaceAll("^get|is", ""));
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
			public Type getGenericType() {
				return this.method.getGenericReturnType();
			}

			@Override
			public Class<?> getType() {
				return this.method.getReturnType();
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
			public boolean hasAnnotation(final Class<? extends Annotation> annotationClass) {
				return this.method.isAnnotationPresent(annotationClass);
			}

			@Override
			public boolean isPersistentProperty() {
				final int modifiers = this.method.getModifiers();
				return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)
						&& !hasAnnotation(Transient.class) && !hasAnnotation(Formula.class);
			}

			@Override
			public <E, T> void setValue(final E entity, final T value) {
				try {
					if (this.setter == null) {
						final String setterName = "set" + StringUtils.capitalize(this.name);
						final Class<?> paramType = this.method.getReturnType();
						for (final Method m : this.method.getDeclaringClass().getDeclaredMethods()) {
							if (m.getName().equals(setterName) && m.getParameterTypes().length == 1
									&& m.getParameterTypes()[0] == paramType) {
								this.setter = m;
								break;
							}
						}
						if (this.setter == null) {
							throw new IllegalStateException("Can't find setter: " + this.method.getDeclaringClass()
									+ '.' + setterName + '(' + paramType + ')');
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

		}

		@Override
		public <E> Iterable<PropertyAccessor> getDeclaredProperties(final Class<E> inspectedClass) {
			final List<PropertyAccessor> result = new ArrayList<>();
			for (final Method method : inspectedClass.getDeclaredMethods()) {
				if (!Modifier.isStatic(method.getModifiers())) {
					final String name = method.getName();
					if (name.startsWith("get") || name.startsWith("is")) {
						result.add(new Accessor(method));
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
	 * Finds all properties of the current access type that are referenced in the given class.
	 *
	 * @param inspectedClass
	 *            the class to inspect
	 * @return all non static properties found in the given class
	 */
	public abstract <E> Iterable<PropertyAccessor> getDeclaredProperties(final Class<E> inspectedClass);

}