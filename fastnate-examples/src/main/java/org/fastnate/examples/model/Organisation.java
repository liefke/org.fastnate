package org.fastnate.examples.model;

import javax.persistence.Entity;
import javax.persistence.Id;

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

	/** The name of the organisation. */
	private String name;

	/** The URL to the website of the organisation. */
	private String url;

	@Override
	public String toString() {
		return this.name;
	}

}
