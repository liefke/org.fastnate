package org.fastnate.generator.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Contains the metadata for a column from a {@link GeneratorTable}.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public class GeneratorColumn extends NamedObject {

	/** The table of this column. */
	@Getter
	private final GeneratorTable table;

	/** The index of this column in the list of all columns of the associated table. */
	@Getter
	private final int index;

	/** The name of this column. */
	@Getter
	private final String name;

	/**
	 * Indicates that the values of this column are not part of an insert statement, because they are generated by the
	 * database.
	 */
	private final boolean autoGenerated;

}
