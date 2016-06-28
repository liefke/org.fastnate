package org.fastnate.generator.test.inheritance;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The superclass of {@link JoinedSubclassTestEntity} to test {@link InheritanceType#JOINED}.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
public class JoinedSuperclassTestEntity extends MappedSuperclassTestEntity implements SuperclassEntity {

	private String name;

	@OneToOne
	private JoinedSuperclassTestEntity superReference;

	@OneToOne
	private JoinedSubclassTestEntity subReference;

	/**
	 * Creates a new instance of {@link JoinedSuperclassTestEntity}.
	 *
	 * @param name
	 *            the name of the new entity
	 * @param superProperty
	 *            the property for the mapped superclass that is saved in our table
	 */
	public JoinedSuperclassTestEntity(final String name, final String superProperty) {
		super(superProperty);
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
