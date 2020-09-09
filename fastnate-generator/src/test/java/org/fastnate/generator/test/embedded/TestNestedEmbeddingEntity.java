package org.fastnate.generator.test.embedded;

import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;

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
