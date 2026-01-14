package org.fastnate.generator.test.embedded;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An embeddable class that embeds another.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TestNestedEmbeddedParent {

	private String description;

	@Embedded
	@AttributeOverride(name = "description", column = @Column(name = "nestedDescription"))
	private TestNestedEmbeddedChild child;

}