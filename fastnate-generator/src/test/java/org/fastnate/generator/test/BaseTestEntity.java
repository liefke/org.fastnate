package org.fastnate.generator.test;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Simple base class for entitities.
 *
 * @author Tobias Liefke
 */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseTestEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Override
	public boolean equals(final Object obj) {
		return this.id == null ? this == obj : obj instanceof BaseTestEntity
				&& (getClass().isInstance(obj) || obj.getClass().isInstance(this))
				&& this.id.equals(((BaseTestEntity) obj).id);
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
