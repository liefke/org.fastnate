package org.fastnate.generator.test.embedded;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

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