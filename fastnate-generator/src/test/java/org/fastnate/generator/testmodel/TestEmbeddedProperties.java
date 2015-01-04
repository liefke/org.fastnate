package org.fastnate.generator.testmodel;

import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An embedded property of an entry.
 * 
 * @author Tobias Liefke
 */
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestEmbeddedProperties {

	private String description;

	@OneToOne
	private PrimitiveTestEntity otherNode;
}