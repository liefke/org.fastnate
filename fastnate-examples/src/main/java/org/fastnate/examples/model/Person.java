package org.fastnate.examples.model;

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple example of an entity for instances that are created with Java code.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Person {

	/** The ID of the person - automatically assigned. */
	@Id
	@GeneratedValue
	private Long id;

	/** The first name of the person - required according to JPA annotation. */
	@Column(nullable = false)
	private String firstName;

	/** The last name of the person - required according to Beans Validation. */
	@NotNull
	private String lastName;

	/** The associated organisation - as entity reference. */
	@ManyToOne
	private Organisation organisation;

	/** The immediate superior of this person. */
	@ManyToOne
	@Setter(AccessLevel.NONE)
	private Person supervisor;

	/** All persons that have this person as supervisor. */
	@OneToMany(mappedBy = "supervisor")
	@Setter(AccessLevel.NONE)
	private final Collection<Person> subordinates = new HashSet<>();

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

	/**
	 * Sets the supervisor of this {@link Person}.
	 *
	 * Removes this person from the {@code subordinates} of the previous supervisor.
	 *
	 * @param supervisor
	 *            the new supervisor
	 */
	public void setSupervisor(final Person supervisor) {
		if (this.supervisor != null) {
			this.supervisor.getSubordinates().remove(this);
		}
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.getSubordinates().add(this);
		}
	}

	@Override
	public String toString() {
		return this.firstName + ' ' + this.lastName;
	}
}