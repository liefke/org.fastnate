package org.fastnate.data.test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

import org.fastnate.data.DataProvider;
import org.fastnate.data.EntityImporter;

/**
 * Data provider to test the {@link EntityImporter}.
 *
 * @author Tobias Liefke
 */
@Getter
public class TestData implements DataProvider {

	/**
	 * A map to let further data providers access our entities.
	 *
	 * A linked hash map to ensure the same order on every execution.
	 */
	private final Map<String, TestEntity> testEntities = new LinkedHashMap<>();

	@Override
	public void buildEntities() throws IOException {
		final TestEntity root = createEntity(null, "Root");
		createEntity(root, "Child1");
		createEntity(root, "Child2");
	}

	/**
	 * Creates a test entity and remembers that one.
	 *
	 * @param name
	 *            the name of the new entity
	 * @param parent
	 *            the parent of the new entity
	 * @return the new entity
	 */
	private TestEntity createEntity(final TestEntity parent, final String name) {
		final TestEntity entity = new TestEntity(parent, name);
		this.testEntities.put(name, entity);
		return entity;
	}

	@Override
	public Collection<TestEntity> getEntities() {
		return this.testEntities.values();
	}

}
