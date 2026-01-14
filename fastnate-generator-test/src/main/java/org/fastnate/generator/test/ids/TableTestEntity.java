package org.fastnate.generator.test.ids;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TableGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses a table for generating its primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@NoArgsConstructor
@Entity
public class TableTestEntity extends IdTestEntity<TableTestEntity> {

	private static final int ALLOCATION_SIZE = 7;

	@Id
	@TableGenerator(name = "TableTest", table = "TestIdTable", allocationSize = ALLOCATION_SIZE)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "TableTest")
	private Long id;

	@ManyToOne
	@Setter
	private TableTestEntity other;

	/**
	 * Creates a new instance of {@link TableTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public TableTestEntity(final String name) {
		super(name);
	}

}
