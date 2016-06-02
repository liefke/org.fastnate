package org.fastnate.generator.test.collections;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.fastnate.generator.test.BaseTestEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class ChildTestEntity extends BaseTestEntity {

	@ManyToOne(optional = false)
	private CollectionsTestEntity parent;

	private String name;

}
