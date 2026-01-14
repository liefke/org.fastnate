package org.fastnate.generator.test.access;

import jakarta.persistence.Access;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * An example entity that has an explicit {@link Access} annotation.
 *
 * @author Tobias Liefke
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ImplicitAccessTestEntity {

	private Long id;

	private String label;

	/**
	 * Creates a new instance of {@link ImplicitAccessTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public ImplicitAccessTestEntity(final String name) {
		this.label = name;
	}

	/**
	 * The id of this {@link ImplicitAccessTestEntity}.
	 *
	 * @return the id
	 */
	@Id
	@GeneratedValue
	public Long getId() {
		return this.id;
	}

	/**
	 * The name of this {@link ImplicitAccessTestEntity}.
	 *
	 * Uses a different field to ensure, that only this method is used.
	 *
	 * @return the name
	 */
	@Column(name = "someName")
	public String getName() {
		return this.label;
	}

	/**
	 * Sets the id of this {@link ImplicitAccessTestEntity}.
	 *
	 * @param id
	 *            the new id to set
	 */
	protected void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Sets the name of this {@link ImplicitAccessTestEntity}.
	 *
	 * @param name
	 *            the new name to set
	 */
	public void setName(final String name) {
		this.label = name;
	}

}
