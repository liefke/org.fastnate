package org.fastnate.examples.model;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

import org.fastnate.examples.data.PersonData;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple example of an entity for instances that are created with Java code or imported from files.
 *
 * See {@link PersonData} and "src/manin/data" for more.
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

	/** The previous organisations of the person. */
	@ManyToMany
	private final Collection<Organisation> previousOrganisations = new HashSet<>();

	/** The immediate superior of this person. */
	@ManyToOne
	private Person supervisor;

	/** All persons that have this person as supervisor. */
	@OneToMany(mappedBy = "supervisor")
	private final Collection<Person> subordinates = new HashSet<>();

	@Temporal(TemporalType.DATE)
	private Date entryDate;

	private boolean active;

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