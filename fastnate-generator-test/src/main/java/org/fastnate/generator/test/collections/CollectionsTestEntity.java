package org.fastnate.generator.test.collections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.fastnate.generator.test.BaseTestEntity;
import org.fastnate.generator.test.SimpleTestEntity;
import org.fastnate.generator.test.primitive.TestEnum;

import lombok.Getter;
import lombok.Setter;

/**
 * An entity for testing collections in SQL generation.
 *
 * @author Tobias Liefke
 */
@Entity(name = "CTE")
@Table(name = "CollTest")
@Getter
@Setter
public class CollectionsTestEntity extends BaseTestEntity {

	@ElementCollection
	private Set<String> stringSet = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "STRING_LIST", joinColumns = @JoinColumn(name = "myEntityId"))
	@Column(name = "stringColumn")
	private List<String> stringList = new ArrayList<>();

	@ElementCollection
	@OrderColumn
	private List<String> orderedStringList = new ArrayList<>();

	@OrderBy
	@Enumerated
	@ElementCollection
	private List<TestEnum> enumList = new ArrayList<>();

	@Embedded
	@OrderColumn
	@ElementCollection
	private List<CollectionsTestEntityProperty> embeddedList = new ArrayList<>();

	@ManyToMany
	private Set<SimpleTestEntity> entitySet = new HashSet<>();

	@OneToMany
	@JoinTable(name = "ENTITY_LIST")
	private List<SimpleTestEntity> entityList = new ArrayList<>();

	@OneToMany
	@JoinTable(name = "OE_LIST", joinColumns = @JoinColumn(name = "join_id"))
	@OrderColumn(name = "sorting")
	private List<SimpleTestEntity> orderedEntityList = new ArrayList<>();

	@OneToMany(mappedBy = "parent")
	@OrderColumn
	private List<ChildTestEntity> children = new ArrayList<>();

	@ManyToMany
	private Set<ChildTestEntity> otherChildren = new HashSet<>();

	@ElementCollection
	private Set<CollectionsTestElement> elements = new HashSet<>();

	@ManyToOne
	private CollectionsTestEntity other;

	@Convert(converter = StringListConverter.class)
	private List<String> convertedStrings = new ArrayList<>();

}
