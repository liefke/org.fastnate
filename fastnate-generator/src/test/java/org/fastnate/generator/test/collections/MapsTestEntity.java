package org.fastnate.generator.test.collections;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyTemporal;
import javax.persistence.OneToMany;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;

import org.fastnate.generator.test.BaseTestEntity;
import org.fastnate.generator.test.SimpleTestEntity;

/**
 * An entity for testing maps in SQL generation.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
public class MapsTestEntity extends BaseTestEntity {

	@ElementCollection
	private Map<String, String> stringMap = new HashMap<>();

	@ElementCollection
	@CollectionTable(name = "DATE_MAP", joinColumns = @JoinColumn(name = "myEntityId"))
	@MapKeyColumn(name = "keyColumn")
	@MapKeyTemporal(TemporalType.DATE)
	@Column(name = "valueColumn")
	private Map<Date, Integer> dateMap = new HashMap<>();

	@OneToMany
	@CollectionTable(name = "ENTITY_MAP", joinColumns = @JoinColumn(name = "myEntityId"))
	@MapKeyJoinColumn(name = "entityColumn")
	private Map<SimpleTestEntity, SimpleTestEntity> entityMap = new HashMap<>();

}
