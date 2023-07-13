package org.fastnate.generator.test.inheritance;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapped superclasses to test inheritance in entities.
 *
 * @author Tobias Liefke
 * @param <E>
 *            a generic type variable to test generic binding
 */
@Getter
@NoArgsConstructor
@MappedSuperclass
public class MappedSuperclassTestEntity<E> {

	@Id
	@GeneratedValue
	private Long id;

	@Setter
	private String superProperty;

	public String getSuperProperty() {
		return superProperty;
	}

	@Setter
	private E genericProperty;

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
		final MappedSuperclassTestEntity<E> other = (MappedSuperclassTestEntity<E>) obj;
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
