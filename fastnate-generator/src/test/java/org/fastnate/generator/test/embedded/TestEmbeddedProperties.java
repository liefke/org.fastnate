package org.fastnate.generator.test.embedded;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import org.fastnate.generator.test.SimpleTestEntity;

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

	@ManyToOne
	private SimpleTestEntity otherNode;
}