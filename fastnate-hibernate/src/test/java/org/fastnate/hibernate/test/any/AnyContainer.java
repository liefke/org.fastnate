package org.fastnate.hibernate.test.any;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OrderColumn;

import org.fastnate.generator.test.BaseTestEntity;
import org.fastnate.generator.test.SimpleTestEntity;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;

import lombok.Getter;
import lombok.Setter;

/**
 * An example entity that tests {@link Any} annotations.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Entity
public class AnyContainer extends BaseTestEntity {

	private static final int DISCRIMINATOR_LENGTH = 3;

	@Any
	@AnyKeyJavaClass(Long.class)
	@JoinColumn(name = "entityId")
	@Column(name = "singleAnyClassId")
	@AnyDiscriminatorValue(entity = SimpleTestEntity.class, discriminator = "1")
	@AnyDiscriminatorValue(entity = AnyContainer.class, discriminator = "2")
	private BaseTestEntity singleAny;

	@Any
	@AnyKeyJavaClass(Long.class)
	@Column(length = DISCRIMINATOR_LENGTH)
	@JoinColumn(name = "anotherAnyId")
	@AnyDiscriminatorValue(entity = SimpleTestEntity.class, discriminator = "STE")
	@AnyDiscriminatorValue(entity = AnyContainer.class, discriminator = "AC")
	private BaseTestEntity anotherAny;

	@ManyToAny
	@MyAnyIntType
	@JoinTable(name = "ManyAny", //
			joinColumns = @JoinColumn(name = "containerId"), //
			inverseJoinColumns = @JoinColumn(name = "entityId"))
	@OrderColumn(name = "orderId")
	private List<BaseTestEntity> manyAny;

	@ManyToAny
	@MyAnyStringType
	@Column(name = "classId")
	@JoinTable(name = "AnyMap", //
			joinColumns = @JoinColumn(name = "containerId"), //
			inverseJoinColumns = @JoinColumn(name = "entityId"))
	private Map<String, BaseTestEntity> anyMap;
}
