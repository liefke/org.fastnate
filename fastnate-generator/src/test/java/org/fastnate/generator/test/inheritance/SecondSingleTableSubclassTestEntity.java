package org.fastnate.generator.test.inheritance;

import jakarta.persistence.Column;
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
public class SecondSingleTableSubclassTestEntity extends MappedSubclassTestEntity implements SubclassEntity {

	@Getter
	@Setter
	@Column(name = "\"value\"")
	private int value;

	/**
	 * Creates a new instance of {@link SecondSingleTableSubclassTestEntity}.
	 *
	 * @param name
	 *            saved in {@link MappedSubclassTestEntity}
	 * @param value
	 *            saved in this class
	 * @param superProperty
	 *            saved in {@link MappedSuperclassTestEntity}
	 */
	public SecondSingleTableSubclassTestEntity(final String name, final int value, final String superProperty) {
		super(name, superProperty);
		this.value = value;
	}

	@Override
	public String getDescription() {
		return String.valueOf(this.value);
	}


}
