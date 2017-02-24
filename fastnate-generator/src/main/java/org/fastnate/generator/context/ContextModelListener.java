package org.fastnate.generator.context;

/**
 * Implementations of this class are interested in the discovery of new {@link EntityClass entity classes} or
 * {@link IdGenerator generators}.
 *
 * @author Tobias Liefke
 */
public interface ContextModelListener {

	/**
	 * Called whenn a new entity class was created.
	 *
	 * @param entityClass
	 *            the found class
	 */
	void foundEntityClass(EntityClass<?> entityClass);

	/**
	 * Called whenn a new generator was created.
	 *
	 * @param generator
	 *            the new generator
	 */
	void foundGenerator(IdGenerator generator);

}
