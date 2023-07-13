package org.fastnate.generator.test.versioning;

import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import org.fastnate.generator.test.BaseTestEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * An entity for testing {@link Version}.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
public class VersionTestEntity extends BaseTestEntity {

	@Version
	private Long ver;

	private String content;

}
