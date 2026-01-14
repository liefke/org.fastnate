package org.fastnate.generator.test.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An embedded property of an entry.
 *
 * @author Tobias Liefke
 */
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestRequiredEmbeddedProperties {

	@Column(nullable = false)
	private String required;

	@Column
	private String optional;
}