package org.fastnate.generator.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.InheritanceType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test class that ensures that an entity hierarchy with {@link InheritanceType#JOINED} is written correctly.
 *
 * @author Tobias Liefke
 */
@Entity
@NoArgsConstructor
public class JoinedSubclassTestEntity extends JoinedSuperclassTestEntity implements SubclassEntity {

	@Getter
	@Setter
	private String description;

	/**
	 * Creates a new instance of {@link JoinedSubclassTestEntity}.
	 *
	 * @param name
	 *            saved in {@link JoinedSuperclassTestEntity}
	 * @param description
	 *            saved in this class
	 * @param superProperty
	 *            saved in {@link MappedSuperclassTestEntity} and written to the table of
	 *            {@link JoinedSuperclassTestEntity}
	 */
	public JoinedSubclassTestEntity(final String name, final String description, final String superProperty) {
		super(name, superProperty);
		this.description = description;
	}

}
