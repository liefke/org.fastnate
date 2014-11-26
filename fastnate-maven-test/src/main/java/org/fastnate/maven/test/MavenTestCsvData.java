package org.fastnate.maven.test;

import java.io.File;

import org.fastnate.data.csv.AbstractCsvDataProvider;

/**
 * Test class for importing CSV files using the maven plugin.
 *
 * @author Tobias Liefke
 */
public class MavenTestCsvData extends AbstractCsvDataProvider<MavenTestEntity> {

	/**
	 * Creates a new instance of {@link MavenTestCsvData}.
	 *
	 * @param dataFolder
	 *            the base folder
	 */
	public MavenTestCsvData(final File dataFolder) {
		super(new File(new File(dataFolder, "csv"), "maventestentities.csv"));
	}

}
