package org.fastnate.generator.test.embedded;

import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to test embedded properties and required fields.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
public class TestNestedEmbeddingEntity {

	@Id
	private long id;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "description", column = @Column(name = "otherDescription")),
			@AttributeOverride(name = "child.testEnum", column = @Column(name = "childEnum")) })
	private TestNestedEmbeddedParent nested;

	@Embedded
	@OrderColumn
	@ElementCollection
	@AttributeOverrides(@AttributeOverride(name = "child.testEnum", column = @Column(name = "nestedChildEnum")))
	private List<TestNestedEmbeddedParent> manyNested;

}
