package org.fastnate.examples.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple example of an entity: a person.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Person {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String firstName;

	@NotNull
	private String lastName;

	/**
	 * Creates a new person with his / her first and last name.
	 *
	 * @param firstName
	 *            the first name of the person
	 * @param lastName
	 *            the last name of the person
	 */
	public Person(final String firstName, final String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public boolean equals(final Object obj) {
		return this.id == null ? obj == this : obj instanceof Person && this.id.equals(((Person) obj).id);
	}

	@Override
	public int hashCode() {
		return this.id == null ? super.hashCode() : this.id.hashCode();
	}

	@Override
	public String toString() {
		return this.firstName + ' ' + this.lastName;
	}
}