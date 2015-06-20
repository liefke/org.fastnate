package org.fastnate.data;

import java.io.File;

/**
 * Used to check if the generation of the SQL file has to start, because a changed file contains relevant data.
 *
 * @author Tobias Liefke
 */
public interface DataChangeDetector {

	/**
	 * Indicates that the given file contains relevant data for the SQL generation.
	 *
	 * @param file
	 *            the (source) file to check
	 * @return {@code true} if the given file is relevant for SQL generation
	 */
	boolean isDataFile(final File file);

}
