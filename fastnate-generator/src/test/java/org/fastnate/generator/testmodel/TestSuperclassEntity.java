package org.fastnate.generator.testmodel;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Entity class to test mapped superclass properties.
 * 
 * @author Tobias Liefke
 */
@MappedSuperclass
public class TestSuperclassEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Override
	public boolean equals(final Object obj) {
		if (this.id == null) {
			return this == obj;
		}
		if (!getClass().isAssignableFrom(obj.getClass()) && !obj.getClass().isAssignableFrom(getClass())) {
			return false;
		}
		final TestSuperclassEntity other = (TestSuperclassEntity) obj;
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
