package org.fastnate.generator.test.embedded;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

import org.fastnate.generator.test.SimpleTestEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An embedded property of an entity.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TestEmbeddedProperties {

	private String description;

	@ManyToOne
	private SimpleTestEntity otherEntity;

}