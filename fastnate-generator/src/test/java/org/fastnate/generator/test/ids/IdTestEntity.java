package org.fastnate.generator.test.ids;

import java.io.Serializable;

import jakarta.annotation.Nullable;
import jakarta.persistence.MappedSuperclass;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base class for all test entities that test the writing of ids.
 *
 * @author Tobias Liefke
 * @param <O>
 *            the type of this class for referencing our self in {@link #getOther}
 */
@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
public abstract class IdTestEntity<O extends IdTestEntity<O>> {

	@Getter
	@Setter
	private String name;

	/**
	 * The ID of this entity.
	 *
	 * @return the ID - the type depends on the kind of generation
	 */
	public abstract Serializable getId();

	/**
	 * The other entity of the same type.
	 *
	 * @return another entity, for testing references with the id
	 */
	@Nullable
	public abstract O getOther();

	/**
	 * Sets the other entity.
	 *
	 * @param other
	 *            another entity of the same type, for testing references with the id
	 */
	public abstract void setOther(@Nullable O other);

	@Override
	public String toString() {
		return this.name;
	}

}
