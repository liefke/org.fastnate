package org.fastnate.generator.test.ids;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses an identity column for it's primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@NoArgsConstructor
@Entity
public class IdentityTestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	@Setter
	private String name;

	@ManyToOne
	@Setter
	private IdentityTestEntity other;

	/**
	 * Creates a new instance of {@link IdentityTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public IdentityTestEntity(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
