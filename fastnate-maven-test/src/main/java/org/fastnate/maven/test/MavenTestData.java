package org.fastnate.maven.test;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import jakarta.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.files.DataFolder;
import org.fastnate.data.files.FsDataFolder;

import lombok.Getter;

/**
 * Data provider for the maven test.
 *
 * @author Tobias Liefke
 */
public class MavenTestData extends AbstractDataProvider {

	@Resource
	private DataFolder dataDir;

	@Getter
	private final Collection<MavenTestEntity> entities = new ArrayList<>();

	@Override
	public void buildEntities() {
		this.entities.add(new MavenTestEntity("Test 1"));
		this.entities.add(new MavenTestEntity("Test 2"));
		this.entities.add(new MavenTestEntity("Test 3"));

		// Test that changes to test.properties are respected
		final File testPropertiesFile = new File(((FsDataFolder) this.dataDir).getFolder(),
				"../resources/test.properties");
		this.entities.add(new MavenTestEntity("test.properties last modified: "
				+ DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.ENGLISH)
						.format(new Date(testPropertiesFile.lastModified()))));
	}

}
