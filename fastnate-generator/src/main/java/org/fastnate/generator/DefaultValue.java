package org.fastnate.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.persistence.Entity;

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
	 * <dt>A number</dt>
	 * <dd>for all numeric fields</dd>
	 * <dt>CURRENT_TIMESTAMP, CURRENT_DATE or a ISO 8601 compliant timestamp (yyyy-mm-ddThh:mm:ss.SX)</dt>
	 * <dd>for temporal fields like date or timestamp</dd>
	 * <dt>an SQL expression which is used "as is" during export</dt>
	 * <dd>for all other fields</dd>
	 * </dl>
	 *
	 * @return the default value
	 */
	String value();

}
