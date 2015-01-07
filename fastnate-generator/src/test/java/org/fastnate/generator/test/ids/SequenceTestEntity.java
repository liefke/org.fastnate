package org.fastnate.generator.test.ids;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses a sequence for it's primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@NoArgsConstructor
@Entity
public class SequenceTestEntity {

	@Id
	@SequenceGenerator(name = "SequenceTest", sequenceName = "testSequence")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SequenceTest")
	private Long id;

	@Column
	@Setter
	private String name;

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
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
