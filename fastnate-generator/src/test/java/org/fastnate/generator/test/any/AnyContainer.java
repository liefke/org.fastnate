package org.fastnate.generator.test.any;

import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OrderColumn;

import org.fastnate.generator.test.BaseTestEntity;
import org.fastnate.generator.test.SimpleTestEntity;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

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
@AnyMetaDef(name = "AnyMetaDefInClass", idType = "long", metaType = "string", metaValues = {
		@MetaValue(targetEntity = SimpleTestEntity.class, value = "SimpleTestEntity"), //
		@MetaValue(targetEntity = AnyContainer.class, value = "AnyContainer") })
public class AnyContainer extends BaseTestEntity {

	@Any(metaColumn = @Column(name = "classId"))
	@AnyMetaDef(idType = "long", metaType = "long", metaValues = {
			@MetaValue(targetEntity = SimpleTestEntity.class, value = "1"),
			@MetaValue(targetEntity = AnyContainer.class, value = "2") })
	@JoinColumn(name = "entityId")
	private BaseTestEntity singleAny;

	@ManyToAny(metaColumn = @Column(name = "classId"), metaDef = "AnyMetaDefInClass")
	@JoinTable(name = "ManyAny", //
			joinColumns = @JoinColumn(name = "containerId"), //
			inverseJoinColumns = @JoinColumn(name = "entityId"))
	@OrderColumn(name = "orderId")
	private List<BaseTestEntity> manyAny;

	@ManyToAny(metaColumn = @Column(name = "classId"), metaDef = "AnyMetaDefInPackage")
	@JoinTable(name = "AnyMap", //
			joinColumns = @JoinColumn(name = "containerId"), //
			inverseJoinColumns = @JoinColumn(name = "entityId"))
	private Map<String, BaseTestEntity> anyMap;
}
