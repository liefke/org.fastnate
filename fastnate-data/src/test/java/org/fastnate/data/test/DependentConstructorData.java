package org.fastnate.data.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A data provider that depends on another data provider in its constructor.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public class DependentConstructorData extends AbstractDataProvider {

	/** The preceding data provider that contains existing entities. */
	private final TestData existingData;

	/** An additionally injected resource. */
	@Resource
	private JaxbTestData csvTestData;

	/** A list that contains all the created data. */
	private final List<TestEntity> entities = new ArrayList<>();

	@Override
	public void buildEntities() throws IOException {
		// Use an existing entity from the preceding data provider to create our test entity
		this.entities
				.add(new TestEntity(this.existingData.getTestEntities().get("Child1"), "DependentConstructorChild"));
	}

	@Override
	public int getOrder() {
		// Even if we return _1_ here, we will be added after CsvTestData, because we depend on it
		return JaxbTestData.ORDER - 1;
	}

}
