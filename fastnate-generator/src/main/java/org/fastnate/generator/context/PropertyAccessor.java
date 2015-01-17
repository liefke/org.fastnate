package org.fastnate.generator.context;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Offers access to a persistent property in an entity class.
 *
 * Instances are created using one of the {@link AccessStyle}s.
 *
 * @author Tobias Liefke
 */
public interface PropertyAccessor {

	/**
	 * Finds an annotation of the property.
	 *
	 * @param annotationClass
	 *            the type of annotation to find
	 * @return the annotation or {@code null} if none was found
	 */
	<A extends Annotation> A getAnnotation(final Class<A> annotationClass);

	/**
	 * The type of the accessed property.
	 *
	 * @return the type of the field or the return type of the method
	 */
	Class<?> getType();

	/**
	 * Resolves the current value for the property on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @return the value, {@code null} if entity is {@code null}
	 */
	<E, T> T getValue(final E entity);

	/**
	 * Checks if the property has a specific annotation.
	 *
	 * @param annotationClass
	 *            the type of annotation to find
	 * @return {@code true} if that annotation was found
	 */
	boolean hasAnnotation(final Class<? extends Annotation> annotationClass);

	/**
	 * Indicates that this property is written to the database.
	 *
	 * @return {@code true} if the represented property is neither static, nor transient, nor generated.
	 */
	boolean isPersistentProperty();

	/**
	 * Sets a new value for the property on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @param value
	 *            the new value
	 */
	<E, T> void setValue(final E entity, final T value);

	/**
	 * The generic type of the accessed property.
	 *
	 * @return the generic type of the field or the generic return type of the method
	 */
	Type getGenericType();

	/**
	 * The name of the accessed property.
	 *
	 * @return the name ({@link Introspector#decapitalize(String) decapitalized})
	 */
	String getName();

	/**
	 * Describes the type of access this property uses.
	 *
	 * @return the type that has created this property accessor
	 */
	AccessStyle getAccessStyle();

}
