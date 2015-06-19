package org.fastnate.maven.test;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Getter;

import org.fastnate.data.AbstractDataProvider;

/**
 * Data provider for the maven test.
 *
 * @author Tobias Liefke
 */
public class MavenTestData extends AbstractDataProvider {

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
