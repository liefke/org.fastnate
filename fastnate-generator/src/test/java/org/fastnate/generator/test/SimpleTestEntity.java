package org.fastnate.generator.test;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple test entity to use in other entities to test references.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimpleTestEntity extends BaseTestEntity {

	@NotNull
	@Size(min = 1)
	private String name;

	/**
	 * Creates a new instance of {@link SimpleTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public SimpleTestEntity(final String name) {
		this.name = name;
	}

}
