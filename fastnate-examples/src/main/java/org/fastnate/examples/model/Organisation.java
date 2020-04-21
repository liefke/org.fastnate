package org.fastnate.examples.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.fastnate.examples.data.OrganisationData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple example of an entity that is imported from files.
 *
 * See {@link OrganisationData} and "src/manin/data" for more.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Organisation {

	/** The ID of the organisation from the input files. */
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

	/** The child organisations. */
	@OneToMany(mappedBy = "parent")
	private List<Organisation> children;

	@Override
	public String toString() {
		return this.name;
	}

}
