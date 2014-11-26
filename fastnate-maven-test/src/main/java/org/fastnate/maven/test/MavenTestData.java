package org.fastnate.maven.test;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Getter;

import org.fastnate.data.DataProvider;

/**
 * Data provider for the maven test.
 *
 * @author Tobias Liefke
 */
public class MavenTestData implements DataProvider {

	@Getter
	private final Collection<MavenTestEntity> entities = new ArrayList<>();

	@Override
	public void buildEntities() {
		this.entities.add(new MavenTestEntity("Test 1"));
		this.entities.add(new MavenTestEntity("Test 2"));
		this.entities.add(new MavenTestEntity("Test 3"));
		this.entities.add(new MavenTestEntity("Test 4"));
	}

}
