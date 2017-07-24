package org.fastnate.generator.context;

/**
 * Implementations of this class are interested in the discovery of new {@link EntityClass entity classes} or
 * {@link IdGenerator generators}.
 *
 * @author Tobias Liefke
 */
public interface ContextModelListener {

	/**
	 * Called when a new table column was discovered.
	 *
	 * @param column
	 *            the new column
	 */
	void foundColumn(GeneratorColumn column);

	/**
	 * Called when a new entity class was discovered.
	 *
	 * @param entityClass
	 *            the found class
	 */
	void foundEntityClass(EntityClass<?> entityClass);

	/**
	 * Called when a new generator was discovered.
	 *
	 * @param generator
	 *            the new generator
	 */
	void foundGenerator(IdGenerator generator);

	/**
	 * Called when a new table was discovered.
	 *
	 * Attention: The columns are not discovered at this point.
	 *
	 * @param table
	 *            the new table
	 */
	void foundTable(GeneratorTable table);

}
