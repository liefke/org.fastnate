package org.fastnate.maven.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for the maven test.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class MavenTestEntity {

	@Id
	@GeneratedValue
	private Long id;

	@Column(unique = true)
	private String name;

	/**
	 * Creates a new instance of {@link MavenTestEntity}.
	 *
	 * @param name
	 *            the name of this entity
	 */
	public MavenTestEntity(final String name) {
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		return this.id == null ? obj == this : obj instanceof MavenTestEntity
				&& this.id.equals(((MavenTestEntity) obj).id);
	}

	@Override
	public int hashCode() {
		return this.id == null ? super.hashCode() : this.id.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}

}
