package org.fastnate.examples.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple example of an entity that is imported from a CSV file.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Organisation {

	/** The ID of the organisation from CSV file. */
	@Id
	private Long id;

	/** The parent organisation. */
	@ManyToOne
	private Organisation parent;

	/** The name of the organisation. */
	@Column(unique = true)
	private String name;

	/** The URL to the website of the organisation. */
	private String url;

	/** The profit of this organisation. */
	private float profit;

	@Override
	public String toString() {
		return this.name;
	}

}
