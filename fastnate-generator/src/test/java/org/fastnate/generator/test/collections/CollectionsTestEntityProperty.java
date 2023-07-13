package org.fastnate.generator.test.collections;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

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
public class CollectionsTestEntityProperty {

	private String description;

	@ManyToOne
	private CollectionsTestEntity otherEntity;
}