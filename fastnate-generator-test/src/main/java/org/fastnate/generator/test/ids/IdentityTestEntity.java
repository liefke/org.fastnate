package org.fastnate.generator.test.ids;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses an identity column for generating its primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@NoArgsConstructor
@Entity
public class IdentityTestEntity extends IdTestEntity<IdentityTestEntity> {

	@Id
	// Comment out for Oracle tests:
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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
		super(name);
	}

}
