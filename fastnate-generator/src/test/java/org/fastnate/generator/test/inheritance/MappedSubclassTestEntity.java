package org.fastnate.generator.test.inheritance;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.InheritanceType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity that is the subclass of a mapped super class.
 *
 * Additionally this is the superclass of {@link SingleTableSubclassTestEntity} to test
 * {@link InheritanceType#SINGLE_TABLE}, the default entity inheritance type.
 *
 * @author Tobias Liefke
 */
@Entity
@NoArgsConstructor
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER)
public class MappedSubclassTestEntity extends MappedSuperclassTestEntity<Integer> implements SuperclassEntity {

	@Setter
	private String name;
	public String getName() {
		return this.name;
	}

	/**
	 * Creates a new instance of {@link MappedSubclassTestEntity}.
	 *
	 * @param name
	 *            the name of the new entity
	 * @param superProperty
	 *            a property that is written, because it was defined in the superclass
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
