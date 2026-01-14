package org.fastnate.data.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.fastnate.data.AbstractDataProvider;

import lombok.Getter;

/**
 * Data Provider to test {@link Inject}.
 *
 * @author Tobias Liefke
 */
public class InjectTestData extends AbstractDataProvider {

	/** Used to test that {@link Inject} works for fields. */
	@Inject
	private TestData existingData;

	@Getter
	private final List<TestEntity> entities = new ArrayList<>();

	private DependentResourceData dependentResourceData;

	@Override
	public void buildEntities() throws IOException {
		// Use an existing entity from the preceding data provider to create our test entity
		this.entities.add(new TestEntity(this.existingData.getTestEntities().get("Child2"), "Injected Child"));
		this.entities.add(new TestEntity(this.dependentResourceData.getEntities().get(0), "Injected Child 2"));
	}

	/**
	 * Used to test that {@link Inject} works for methods as well.
	 *
	 * @param dependentResourceData
	 *            the other data provider
	 */
	@Inject
	public void setDependentResourceData(final DependentResourceData dependentResourceData) {
		this.dependentResourceData = dependentResourceData;
	}

}
