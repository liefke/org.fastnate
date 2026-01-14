package org.fastnate.generator.test.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * An example entity that has an explicit {@link Access} annotation.
 *
 * @author Tobias Liefke
 */
@Access(AccessType.PROPERTY)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
public class ExplicitAccessTestEntity {

	private long id;

	private String label;

	/**
	 * The id of this {@link ExplicitAccessTestEntity}.
	 *
	 * @return the id
	 */
	@Id
	public long getId() {
		return this.id;
	}

	/**
	 * The name of this {@link ExplicitAccessTestEntity}.
	 *
	 * Uses a different field to ensure, that only this method is used.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.label;
	}

	/**
	 * Sets the id of this {@link ExplicitAccessTestEntity}.
	 *
	 * @param id
	 *            the new id to set
	 */
	protected void setId(final long id) {
		this.id = id;
	}

	/**
	 * Sets the name of this {@link ExplicitAccessTestEntity}.
	 *
	 * @param name
	 *            the new name to set
	 */
	public void setName(final String name) {
		this.label = name;
	}

}
