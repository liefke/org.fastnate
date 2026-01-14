package org.fastnate.generator.test.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
	@SequenceGenerator(name = "SchemaTestSequence", //
			catalog = "fastnate", schema = "SequenceSchema", sequenceName = "STSeq")
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
