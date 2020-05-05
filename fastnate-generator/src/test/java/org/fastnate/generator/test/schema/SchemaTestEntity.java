package org.fastnate.generator.test.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test entity for testing {@link Table#schema() schema} and {@link Table#catalog() catalog} annotations.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@Table(catalog = "fastnate", schema = "MySchema", name = "SchemaTest")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchemaTestEntity {

	@Id
	@GeneratedValue(generator = "SchemaTestSequence")
	@SequenceGenerator(name = "SchemaTestSequence", schema = "MySchema", sequenceName = "STSeq")
	private Long id;

	@NotNull
	@Size(min = 1)
	private String name;

	@OneToMany
	@JoinTable(catalog = "fastnate", schema = "ListSchema")
	private List<SchemaTestEntity> entities = new ArrayList<>();

	@ElementCollection
	@CollectionTable(catalog = "fastnate", schema = "SetSchema")
	private Set<String> strings = new HashSet<>();

	/**
	 * Creates a new instance of {@link SchemaTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public SchemaTestEntity(final String name) {
		this.name = name;
	}

}
