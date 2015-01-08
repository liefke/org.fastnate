package org.fastnate.generator.test.embedded;

import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.fastnate.generator.test.SimpleTestEntity;

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
	private SimpleTestEntity otherNode;
}