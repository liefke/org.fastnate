package org.fastnate.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.Entity;

/**
 * Annotation for attributes of {@link Entity entities}, which indicates default values during SQL generation.
 * 
 * Attention: This annotation is not evaluated by JPA - neither the JPA provider nor the database will use these values
 * during runtime.
 * 
 * @author Tobias Liefke
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DefaultValue {

	/**
	 * The value to use during export.
	 * 
	 * Depending on the type of the field:
	 * <dl>
	 * <dt>a plain string which will be escaped during export</dt>
	 * <dd>for fields of type {@link String}, {@link Character} and {@code char}</dd>
	 * <dt>the name of an enum constant</dt>
	 * <dd>for fields of type {@link Enum}</dd>
	 * <dt>a SQL expression which is used "as is" during export</dt>
	 * <dd>for all other fields</dd>
	 * </dl>
	 */
	String value();

}