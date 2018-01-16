package org.fastnate.generator.test.reference;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Child entity to test references between parents and childs.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequiredChildTestEntity {

	@Id
	private Long id;

	@ManyToOne
	@JoinColumn(name = "parentId")
	private ParentTestEntity parent;

}
