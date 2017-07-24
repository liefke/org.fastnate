package org.fastnate.generator.context;

/**
 * Base implementation of a {@link ContextModelListener} with empty methods.
 *
 * @author Tobias Liefke
 */
public class DefaultContextModelListener implements ContextModelListener {

	@Override
	public void foundColumn(final GeneratorColumn column) {
		// Empty method stub
	}

	@Override
	public void foundEntityClass(final EntityClass<?> entityClass) {
		// Empty method stub
	}

	@Override
	public void foundGenerator(final IdGenerator generator) {
		// Empty method stub
	}

	@Override
	public void foundTable(final GeneratorTable table) {
		// Empty method stub
	}

}
