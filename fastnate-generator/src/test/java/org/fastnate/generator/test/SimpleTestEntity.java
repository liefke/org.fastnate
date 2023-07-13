package org.fastnate.generator.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
@Table(name = "SimpleTest")
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
