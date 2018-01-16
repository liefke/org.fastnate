package org.fastnate.generator.test.reference;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parent entity to test references between parents and childs.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ParentTestEntity {

	@Id
	private Long id;

	@NotNull
	@OneToOne
	@JoinColumn(name = "requiredChildId")
	private RequiredChildTestEntity requiredChild;

}
