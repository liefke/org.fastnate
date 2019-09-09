package org.fastnate.data.files;

import java.io.IOException;
import java.io.InputStream;

import org.fastnate.data.DataProvider;

/**
 * Represents one file from the class path or the file system which is imported with a {@link DataProvider}.
 *
 * @author Tobias Liefke
 */
public interface DataFile {

	/**
	 * The folder that contains this file.
	 *
	 * @return the folder
	 */
	DataFolder getFolder();

	/**
	 * The name of this file.
	 *
	 * @return the name of this file
	 */
	String getName();

	/**
	 * Opens this file for input.
	 *
	 * The caller is responsible to close the stream.
	 *
	 * @return the input stream of this file
	 * @throws IOException
	 *             if the file is not accessible
	 */
	InputStream open() throws IOException;

}
