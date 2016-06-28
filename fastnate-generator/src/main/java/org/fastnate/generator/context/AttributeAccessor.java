package org.fastnate.generator.context;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * Offers access to a persistent attribute in an entity class.
 *
 * Instances are created using one of the {@link AccessStyle}s.
 *
 * @author Tobias Liefke
 */
public interface AttributeAccessor extends AnnotatedElement {

	/**
	 * Describes how this attribute accesses the values below.
	 *
	 * @return the type that has created this attribute accessor
	 */
	AccessStyle getAccessStyle();

	/**
	 * The actual field or method.
	 *
	 * @return the wrapped element of the attribute
	 */
	AnnotatedElement getElement();

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
	 * Indicates that this attribute is written to the database.
	 *
	 * @return {@code true} if the represented attribute is neither static, nor transient, nor generated.
	 */
	boolean isPersistent();

	/**
	 * Sets a new value for the attribute on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @param value
	 *            the new value
	 */
	<E, T> void setValue(final E entity, final T value);

}
