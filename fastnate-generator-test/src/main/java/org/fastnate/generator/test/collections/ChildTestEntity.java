package org.fastnate.generator.test.collections;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import org.fastnate.generator.test.BaseTestEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An entity that is included in {@link CollectionsTestEntity} to test mapped-by constructs.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChildTestEntity extends BaseTestEntity {

	@ManyToOne(optional = false)
	private CollectionsTestEntity parent;

	private String name;

	@ManyToMany(mappedBy = "otherChildren")
	private Set<CollectionsTestEntity> otherParents;

	/**
	 * Creates a new instance of an entity.
	 * 
	 * @param parent
	 *            the parent of this entity
	 * @param name
	 *            the name of this entity
	 */
	public ChildTestEntity(final CollectionsTestEntity parent, final String name) {
		this.parent = parent;
		this.name = name;
		this.otherParents = new HashSet<>();
	}

}
