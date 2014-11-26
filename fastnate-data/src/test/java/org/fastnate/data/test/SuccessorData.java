package org.fastnate.data.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.fastnate.data.DataProvider;
import org.fastnate.data.EntityImporter;

/**
 * Data provider to test more than one in the {@link EntityImporter}.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public class SuccessorData implements DataProvider {

	/** The preceding data provider that contains existing entities. */
	private final TestData existingData;

	/** A list that contains all the created data. */
	private final List<TestEntity> entities = new ArrayList<>();

	@Override
	public void buildEntities() throws IOException {
		// Use an existing entity from the preceding data provider to create our test entity
		this.entities.add(new TestEntity(this.existingData.getTestEntities().get("Child1"), "Successor"));
	}

}
