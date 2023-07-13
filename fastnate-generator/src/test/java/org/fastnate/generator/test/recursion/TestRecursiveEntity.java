package org.fastnate.generator.test.recursion;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.fastnate.generator.test.BaseTestEntity;

/**
 * Entity to test recursion in SQL generation.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestRecursiveEntity extends BaseTestEntity {

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
