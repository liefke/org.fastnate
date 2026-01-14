package org.fastnate.generator.test.ids;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses a sequence for generating its primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@NoArgsConstructor
@Entity
public class SequenceTestEntity extends IdTestEntity<SequenceTestEntity> {

	@Id
	@SequenceGenerator(name = "SequenceTest", sequenceName = "testSequence")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SequenceTest")
	private Long id;

	@ManyToOne
	@Setter
	private SequenceTestEntity other;

	/**
	 * Creates a new instance of {@link SequenceTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public SequenceTestEntity(final String name) {
		super(name);
	}

}
