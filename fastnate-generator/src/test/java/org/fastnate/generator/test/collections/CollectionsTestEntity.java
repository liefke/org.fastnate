package org.fastnate.generator.test.collections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

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
@Table(name = "CollectionsTest")
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

	@ElementCollection
	private Set<CollectionsTestElement> elements = new HashSet<>();

	@ManyToOne
	private CollectionsTestEntity other;

}
