package org.fastnate.generator.test.inheritance;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapped superclasses to test inheritance in entities.
 *
 * @author Tobias Liefke
 */
@MappedSuperclass
@Getter
@NoArgsConstructor
public class MappedSuperclassTestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Setter
	private String superProperty;

	/**
	 * Creates a new instance of {@link MappedSuperclassTestEntity}.
	 * 
	 * @param superProperty
	 *            a test property that is written
	 */
	public MappedSuperclassTestEntity(final String superProperty) {
		this.superProperty = superProperty;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this.id == null) {
			return this == obj;
		}
		if (!getClass().isAssignableFrom(obj.getClass()) && !obj.getClass().isAssignableFrom(getClass())) {
			return false;
		}
		final MappedSuperclassTestEntity other = (MappedSuperclassTestEntity) obj;
		return this.id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return this.id == null ? super.hashCode() : this.id.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + (this.id == null ? '@' + hashCode() : this.id);
	}

}
