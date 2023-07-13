package org.fastnate.generator.test.collections;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TemporalType;

import org.fastnate.generator.test.BaseTestEntity;
import org.fastnate.generator.test.SimpleTestEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * An entity for testing maps in SQL generation.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@Table(name = "MapsTest")
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
	private Map<String, SimpleTestEntity> stringToEntityMap = new HashMap<>();

	@OneToMany
	@CollectionTable(name = "ENTITY_MAP", joinColumns = @JoinColumn(name = "myEntityId"))
	@MapKeyJoinColumn(name = "entityColumn")
	private Map<SimpleTestEntity, SimpleTestEntity> entityToEntityMap = new HashMap<>();

}
