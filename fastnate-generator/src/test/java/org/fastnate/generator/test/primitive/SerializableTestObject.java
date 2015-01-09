package org.fastnate.generator.test.primitive;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used to test that serializable objects may be written.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SerializableTestObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private String stringProperty;

	private int intProperty;

}
