package org.fastnate.generator.test.overrides;

import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OrderColumn;

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

	@OrderColumn
	@ElementCollection
	private List<String> stringList;

}
