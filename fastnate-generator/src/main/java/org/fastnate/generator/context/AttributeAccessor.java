package org.fastnate.generator.context;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
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
	 * The class that declared this attribute.
	 *
	 * @return the class that contains the attribute
	 */
	Class<?> getDeclaringClass();

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
	 * The class that is the type of every object that contains this attribute.
	 *
	 * Either {@link #getDeclaringClass() the class that declared this attribute} or a sub class. Used to resolve any
	 * generic type parameter in {@link #getType()}.
	 *
	 * @return the class where the scan of attributes started
	 */
	Class<?> getImplementationClass();

	/**
	 * The combination of {@link Modifier}s.
	 *
	 * @return the modifiers of the field or method
	 */
	int getModifiers();

	/**
	 * The name of the accessed attribute.
	 *
	 * @return the name ({@link Introspector#decapitalize(String) decapitalized} if necessary)
	 */
	String getName();

	/**
	 * The type of the accessed attribute. If the type is a generic type, any type variable is resolved with
	 * {@link #getImplementationClass()}.
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
	<E, T> T getValue(E entity);

	/**
	 * Sets a new value for the attribute on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @param value
	 *            the new value
	 */
	<E, T> void setValue(E entity, T value);

}
