package org.fastnate.maven.test;

import java.io.File;

import org.fastnate.data.DataChangeDetector;

/**
 * Tests the call of the data change detector.
 *
 * @author Tobias Liefke
 */
public class TestDetector implements DataChangeDetector {

	@Override
	public boolean isDataFile(final File file) {
		return file.getName().endsWith(".java");
	}

}
