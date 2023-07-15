package org.fastnate.generator.test.any;

import java.util.List;
import java.util.Map;

import jakarta.persistence.*;

import org.fastnate.generator.test.BaseTestEntity;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.ManyToAny;

import lombok.Getter;
import lombok.Setter;

/**
 * An example entity that has an explicit {@link Access} annotation.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
public class AnyContainer extends BaseTestEntity {

	@Any
	@JoinColumn(name = "entityId")
	@AnyKeyJavaClass(Integer.class)
	@Column(name = "singleAny")
	private BaseTestEntity singleAny;

	@ManyToAny
	@JoinTable(name = "ManyAny", //
			joinColumns = @JoinColumn(name = "containerId"), //
			inverseJoinColumns = @JoinColumn(name = "entityId"))
	@OrderColumn(name = "orderId")
	@AnyKeyJavaClass(Integer.class)
	@Column(name = "ManyAny")
	private List<BaseTestEntity> manyAny;

	@ManyToAny
	@JoinTable(name = "AnyMap", //
			joinColumns = @JoinColumn(name = "containerId"), //
			inverseJoinColumns = @JoinColumn(name = "entityId"))
	@Column(name = "AnyMap")
	@AnyKeyJavaClass(Integer.class)
	private Map<String, BaseTestEntity> anyMap;
}
