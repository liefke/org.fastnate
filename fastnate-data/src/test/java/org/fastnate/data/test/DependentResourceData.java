package org.fastnate.data.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.files.DataFolder;

import lombok.Getter;

/**
 * A data provider that depends on another data provider in its fields.
 *
 * @author Tobias Liefke
 */
@Getter
public class DependentResourceData extends AbstractDataProvider {

	/** The preceding data provider that contains existing entities. */
	@Resource
	private TestData existingData;

	/** The base directory for data imports. */
	private DataFolder dataDir;

	/** A list that contains all the created data. */
	private final List<TestEntity> entities = new ArrayList<>();

	/** A value to test that {@link #init()} was called. */
	private boolean initialized;

	@Override
	public void buildEntities() throws IOException {
		Assertions.assertThat(this.initialized).as("initialized").isTrue();

		// Use an existing entity from the preceding data provider to create our test entity
		this.entities.add(new TestEntity(this.existingData.getTestEntities().get("Child2"), "DependentResourceChild"));
	}

	@Override
	public int getOrder() {
		// We want to run after CsvTestData
		return JaxbTestData.ORDER + 1;
	}

	/**
	 * Called after {@link #existingData} and {@link #dataDir} was injected.
	 */
	@PostConstruct
	private void init() {
		Assertions.assertThat(this.existingData).as("existingData").isNotNull();
		Assertions.assertThat(this.dataDir).as("dataDir").isNotNull();
		this.initialized = true;
	}

	/**
	 * Tests the injection of the data dir using a method.
	 *
	 * @param dataDir
	 *            the data directory of the entity importer
	 */
	@Resource
	public void setDataDir(final DataFolder dataDir) {
		this.dataDir = dataDir;
	}

}
