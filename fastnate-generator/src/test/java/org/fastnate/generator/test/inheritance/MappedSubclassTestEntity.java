package org.fastnate.generator.test.inheritance;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity that is the subclass of a mapped super class.
 *
 * @author Tobias Liefke
 */
@Entity
@NoArgsConstructor
public class MappedSubclassTestEntity extends MappedSuperclassTestEntity {

	@Getter
	@Setter
	private String name;

	/**
	 * Creates a new instance of {@link MappedSubclassTestEntity}.
	 *
	 * @param name
	 *            the name of the new entity
	 */
	public MappedSubclassTestEntity(final String name) {
		this.name = name;
	}

	/**
	 * Creates a new instance of {@link MappedSubclassTestEntity}.
	 *
	 * @param name
	 *            the name of the new entity
	 * @param superProperty
	 *            a property that is written as well
	 */
	public MappedSubclassTestEntity(final String name, final String superProperty) {
		super(superProperty);
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
