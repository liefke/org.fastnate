package org.fastnate.data;

import java.io.IOException;
import java.util.Collection;

import org.fastnate.generator.EntitySqlGenerator;

/**
 * Implementations of this class will automatically instantiated by the {@link EntityImporter}.
 *
 * The constructor must either be the "no arguments constructor" - or accept one or more of the following parameters:
 * <ul>
 * <li>the data directory as {@link java.io.File}</li>
 * <li>the {@link java.util.Properties} settings</li>
 * <li>references to other DataProviders</li>
 * </ul>
 *
 * If references to other DataProviders are used in the constructor, the {@link #buildEntities()} method of these
 * providers are called before our {@link #buildEntities()}.
 *
 * @author Andreas Penski
 * @author Tobias Liefke
 */
public abstract class AbstractDataProvider implements DataProvider {

	/**
	 * The list of entities for which INSERT SQL statements will be generated.
	 *
	 * @return list of entities
	 */
	protected abstract Collection<?> getEntities();

	/**
	 * The order of a data provider in comparison to others, defaults to 0.
	 */
	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * The default implementation just writes all {@link #getEntities() entities} that were created during
	 * {@link #buildEntities()}.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Override
	public void writeEntities(final EntitySqlGenerator sqlGenerator) throws IOException {
		sqlGenerator.write(getEntities());
	}

}
