package org.fastnate.generator.test.embedded;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The embedded id of an entry.
 *
 * @author Tobias Liefke
 */
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestEmbeddedId implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;

	@NotNull
	private String name;

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof TestEmbeddedId)) {
			return false;
		}
		final TestEmbeddedId other = (TestEmbeddedId) obj;
		return this.id == other.id && this.name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return (int) this.id + this.name.hashCode();
	}

}