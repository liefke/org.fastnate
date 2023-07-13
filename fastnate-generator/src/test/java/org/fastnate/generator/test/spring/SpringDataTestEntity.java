package org.fastnate.generator.test.spring;

import jakarta.persistence.Entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity to test that Fastnate supports "Spring Data JPA" entities.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class SpringDataTestEntity extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 1L;

	private String content;

}
