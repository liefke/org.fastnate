package org.fastnate.generator.testmodel;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An embedded property of an entity.
 *
 * @author Tobias Liefke
 */
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestPluralEntityProperty {

	private String description;

	@ManyToOne
	private TestPluralEntity otherEntity;
}