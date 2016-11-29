package org.fastnate.generator.test.ids;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.TableGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses a primitive {@code long} as generated ID.
 *
 * @author Tobias Liefke
 */
@NoArgsConstructor
@Entity
public class PrimitiveIdTestEntity extends IdTestEntity<PrimitiveIdTestEntity> {

	private static final int ALLOCATION_SIZE = 25;

	@Id
	@TableGenerator(name = "PrimitiveIdTest", table = "TestPrimitiveIdTable", allocationSize = ALLOCATION_SIZE)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "PrimitiveIdTest")
	private long id;

	@ManyToOne
	@Setter
	@Getter
	private PrimitiveIdTestEntity other;

	/**
	 * Creates a new instance of {@link PrimitiveIdTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public PrimitiveIdTestEntity(final String name) {
		super(name);
	}

	@Override
	public Number getId() {
		return Long.valueOf(this.id);
	}

}
