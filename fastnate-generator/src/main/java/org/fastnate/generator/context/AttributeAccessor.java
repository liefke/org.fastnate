package org.fastnate.generator.context;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Offers access to a persistent attribute in an entity class.
 *
 * Instances are created using one of the {@link AccessStyle}s.
 *
 * @author Tobias Liefke
 */
public interface AttributeAccessor {

	/**
	 * Finds an annotation of the attribute.
	 *
	 * @param annotationClass
	 *            the type of annotation to find
	 * @return the annotation or {@code null} if none was found
	 */
	<A extends Annotation> A getAnnotation(final Class<A> annotationClass);

	/**
	 * The type of the accessed attribute.
	 *
	 * @return the type of the field or the return type of the method
	 */
	Class<?> getType();

	/**
	 * Resolves the current value for the attribute on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @return the value, {@code null} if entity is {@code null}
	 */
	<E, T> T getValue(final E entity);

	/**
	 * Checks if the attribute has a specific annotation.
	 *
	 * @param annotationClass
	 *            the type of annotation to find
	 * @return {@code true} if that annotation was found
	 */
	boolean hasAnnotation(final Class<? extends Annotation> annotationClass);

	/**
	 * Indicates that this attribute is written to the database.
	 *
	 * @return {@code true} if the represented attribute is neither static, nor transient, nor generated.
	 */
	boolean isPersistentProperty();

	/**
	 * Sets a new value for the attribute on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @param value
	 *            the new value
	 */
	<E, T> void setValue(final E entity, final T value);

	/**
	 * The generic type of the accessed attribute.
	 *
	 * @return the generic type of the field or the generic return type of the method
	 */
	Type getGenericType();

	/**
	 * The name of the accessed attribute.
	 *
	 * @return the name ({@link Introspector#decapitalize(String) decapitalized} if necessary)
	 */
	String getName();

	/**
	 * Describes how this attribute accesses the values below.
	 *
	 * @return the type that has created this attribute accessor
	 */
	AccessStyle getAccessStyle();

}
