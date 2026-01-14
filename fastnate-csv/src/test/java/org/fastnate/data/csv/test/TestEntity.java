package org.fastnate.data.csv.test;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test entity for the CSV tests.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class TestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private TestEntity parent;

	@Column(unique = true)
	private String name;

	@Column(name = "integ")
	private Integer num;

	private Boolean bool;

	private Date date;

	@OneToMany(mappedBy = "parent")
	private Set<TestEntity> children = new LinkedHashSet<>();

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
		if (parent != null) {
			parent.children.add(this);
		}
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
