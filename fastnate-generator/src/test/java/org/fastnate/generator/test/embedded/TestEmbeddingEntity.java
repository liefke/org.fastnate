package org.fastnate.generator.test.embedded;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity to test embedded properties and required fields.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity(name = "EmbedEnty")
public class TestEmbeddingEntity {

	@EmbeddedId
	private TestEmbeddedId id;

	@Embedded
	private TestEmbeddedProperties properties;

	@Embedded
	private TestRequiredEmbeddedProperties required;

}
