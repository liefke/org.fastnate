package org.fastnate.generator.test.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.fastnate.generator.test.BaseTestEntity;

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
@Table(catalog = "test", schema = "MySchema", name = "SchemaTest")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchemaTestEntity extends BaseTestEntity {

	@NotNull
	@Size(min = 1)
	private String name;

	@OneToMany
	@JoinTable(catalog = "test", schema = "SetSchema")
	private Set<SchemaTestEntity> entities = new HashSet<>();

	@ElementCollection
	@CollectionTable(catalog = "test", schema = "ListSchema")
	private List<String> strings = new ArrayList<>();

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
