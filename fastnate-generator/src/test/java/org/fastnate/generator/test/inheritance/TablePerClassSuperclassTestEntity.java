package org.fastnate.generator.test.inheritance;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The superclass of {@link TablePerClassSubclassTestEntity} to test {@link InheritanceType#TABLE_PER_CLASS}.
 *
 * @author Tobias Liefke
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NoArgsConstructor
public class TablePerClassSuperclassTestEntity {

	@Getter
	@Id
	private long id;

	@Getter
	@Setter
	private String name;

	/**
	 * Creates a new instance of {@link TablePerClassSuperclassTestEntity}.
	 *
	 * @param id
	 *            the id of the entity, as identity generation is not supported for this type of inheritance
	 * @param name
	 *            the name of the new entity
	 */
	public TablePerClassSuperclassTestEntity(final long id, final String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof TablePerClassSuperclassTestEntity
				&& ((TablePerClassSuperclassTestEntity) obj).id == this.id;
	}

	@Override
	public int hashCode() {
		return (int) this.id;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
