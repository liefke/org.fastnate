package org.fastnate.generator.test.inheritance;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.InheritanceType;

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
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER)
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
