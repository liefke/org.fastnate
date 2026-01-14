package org.fastnate.generator.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test class that ensures that an entity hierarchy with {@link InheritanceType#SINGLE_TABLE} is written correctly.
 *
 * @author Tobias Liefke
 */
@Entity
@Table(name = "TPCSubclassTestEntity")
@NoArgsConstructor
public class TablePerClassSubclassTestEntity extends TablePerClassSuperclassTestEntity implements SubclassEntity {

	@Getter
	@Setter
	private String description;

	/**
	 * Creates a new instance of {@link TablePerClassSubclassTestEntity}.
	 *
	 * @param id
	 *            the id of the entity, as identity generation is not supported for this type of inheritance
	 * @param name
	 *            saved in {@link TablePerClassSuperclassTestEntity}
	 * @param description
	 *            saved in this class
	 */
	public TablePerClassSubclassTestEntity(final long id, final String name, final String description) {
		super(id, name);
		this.description = description;
	}

}
