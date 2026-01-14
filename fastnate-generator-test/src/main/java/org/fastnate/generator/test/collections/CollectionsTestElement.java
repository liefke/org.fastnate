package org.fastnate.generator.test.collections;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.fastnate.generator.test.embedded.TestEmbeddingEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An element from a collection in {@link TestEmbeddingEntity}.
 *
 * @author Tobias Liefke
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionsTestElement {

	@NotNull
	@ManyToOne
	private CollectionsTestEntity entity;

}
