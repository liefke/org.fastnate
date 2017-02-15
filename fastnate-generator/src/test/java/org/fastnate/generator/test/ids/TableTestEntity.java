package org.fastnate.generator.test.ids;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.TableGenerator;

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
