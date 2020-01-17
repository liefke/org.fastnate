package org.fastnate.generator.test.overrides;

import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.fastnate.generator.test.BaseTestEntity;
import org.fastnate.generator.test.SimpleTestEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * Contains JPA properties that are reconfigured in {@link OverrideEntity}.
 *
 * @author Tobias Liefke
 */
@Setter
@Getter
@MappedSuperclass
@AttributeOverride(name = "id", column = @Column(name = "overridenId"))
public class ToOverrideEntity extends BaseTestEntity {

	@Column(name = "simple")
	private String simpleProperty;

	@ManyToOne
	private OverrideEntity otherEntity;

	@ManyToMany
	private List<SimpleTestEntity> simpleEntities;

}
