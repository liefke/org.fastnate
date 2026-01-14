package org.fastnate.generator.test.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.InheritanceType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test class that ensures that an entity hierarchy with {@link InheritanceType#SINGLE_TABLE} is written correctly.
 *
 * @author Tobias Liefke
 */
@Entity
@NoArgsConstructor
@DiscriminatorValue(value = "2")
public class SingleTableSubclassTestEntity extends MappedSubclassTestEntity implements SubclassEntity {

	@Getter
	@Setter
	private String description;

	/**
	 * Creates a new instance of {@link SingleTableSubclassTestEntity}.
	 *
	 * @param name
	 *            saved in {@link MappedSubclassTestEntity}
	 * @param description
	 *            saved in this class
	 * @param superProperty
	 *            saved in {@link MappedSuperclassTestEntity}
	 */
	public SingleTableSubclassTestEntity(final String name, final String description, final String superProperty) {
		super(name, superProperty);
		this.description = description;
	}

}
