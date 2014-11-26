package org.fastnate.data.test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test entity for the data provider test.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class TestEntity {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	private TestEntity parent;

	@Column(unique = true)
	private String name;

	@Column(name = "integ")
	private Integer number;

	private Boolean bool;

	/**
	 * Creates a new instance of {@link TestEntity}.
	 *
	 * @param parent
	 *            the parent entity
	 * @param name
	 *            the name of this entity
	 */
	public TestEntity(final TestEntity parent, final String name) {
		this.parent = parent;
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		return this.id == null ? obj == this : obj instanceof TestEntity && this.id.equals(((TestEntity) obj).id);
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
