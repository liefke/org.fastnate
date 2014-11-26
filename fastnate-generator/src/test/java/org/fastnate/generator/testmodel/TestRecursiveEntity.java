package org.fastnate.generator.testmodel;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to test primitive properties and recursion in SQL generation.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@NamedQuery(name = TestRecursiveEntity.NQ_ENTITY_BY_NAME, query = "SELECT e FROM TestRecursiveEntity e WHERE e.name = :name")
public class TestRecursiveEntity extends TestSuperclassEntity {

	/**
	 * Name query to find entity by :name.
	 */
	public static final String NQ_ENTITY_BY_NAME = "recursiveEntityByName";

	private static final int NAME_LENGTH = 30;

	@Column(length = NAME_LENGTH, unique = true)
	@NotNull
	@Size(min = 1)
	private String name;

	@ManyToOne
	@Setter(AccessLevel.NONE)
	private TestRecursiveEntity parent;

	@OneToMany(mappedBy = "parent")
	private Collection<TestRecursiveEntity> children = new ArrayList<>();

	/**
	 * Creates a new instance of {@link TestRecursiveEntity}.
	 *
	 * @param parent
	 *            the parent entity
	 * @param name
	 *            the name of the entity
	 */
	public TestRecursiveEntity(final TestRecursiveEntity parent, final String name) {
		this.parent = parent;
		this.name = name;
		if (parent != null) {
			parent.getChildren().add(this);
		}
	}

}
