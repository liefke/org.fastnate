package org.fastnate.generator.test.ids;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses a number as explicit primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@NoArgsConstructor
@Entity
public class FixedIdTestEntity extends IdTestEntity<FixedIdTestEntity> {

	@Id
	private String id;

	@ManyToOne
	@Setter
	private FixedIdTestEntity other;

	/**
	 * Creates a new instance of {@link FixedIdTestEntity}.
	 *
	 * @param id
	 *            the id of the entity
	 */
	public FixedIdTestEntity(final String id) {
		super(id);
		this.id = id;
	}

}
