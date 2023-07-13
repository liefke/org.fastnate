package org.fastnate.generator.test.embedded;

import jakarta.persistence.Embeddable;

import org.fastnate.generator.test.primitive.TestEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An embeddable class that is embededd in another.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TestNestedEmbeddedChild {

	private String description;

	private TestEnum testEnum;

}