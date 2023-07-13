package org.fastnate.generator.test.performance;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.fastnate.generator.test.BaseTestEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity for the {@link PerformanceTest}.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceTestEntity extends BaseTestEntity {

	private String name;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
	@OrderBy("ordering")
	private List<PerformanceTestEntity> children;

	@ManyToOne
	private PerformanceTestEntity parent;

	private int ordering;

	@ElementCollection
	private List<String> strings;

	/**
	 * Creates a new child of an entity.
	 *
	 * @param parent
	 *            the parent of the new child
	 * @param name
	 *            the name of the entity
	 */
	public PerformanceTestEntity(final PerformanceTestEntity parent, final String name) {
		this(name);
		this.parent = parent;
		this.ordering = parent.getChildren().size();
		parent.getChildren().add(this);
	}

	/**
	 * Creates a new root entity.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public PerformanceTestEntity(final String name) {
		this.name = name;
		this.children = new ArrayList<>();
		this.strings = new ArrayList<>();
	}

}
