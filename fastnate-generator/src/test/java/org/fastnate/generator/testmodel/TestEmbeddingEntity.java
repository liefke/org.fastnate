package org.fastnate.generator.testmodel;

import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity to test embedded properties and required fields.
 *
 * @author Tobias Liefke
 */
@Entity(name = "EmbedEnty")
@Getter
@Setter
public class TestEmbeddingEntity {

	@EmbeddedId
	private TestEmbeddedId id;

	@Embedded
	private TestEmbeddedProperties properties;

	@Embedded
	private TestRequiredEmbeddedProperties required;

}
