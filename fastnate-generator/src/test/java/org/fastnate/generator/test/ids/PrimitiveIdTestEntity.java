package org.fastnate.generator.test.ids;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TableGenerator;

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
	private int id;

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
