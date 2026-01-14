package org.fastnate.data.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import jakarta.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.files.DataFolder;
import org.fastnate.data.xml.JaxbImporter;

import lombok.Getter;

/**
 * Tests the manual import from XML.
 *
 * @author Tobias Liefke
 */
public class JaxbTestData extends AbstractDataProvider {

	/** The position of this data provider. */
	public static final int ORDER = 2;

	@Resource
	private DataFolder dataDir;

	@Getter
	private Collection<JaxbTestEntity> entities;

	@Override
	public void buildEntities() throws IOException {
		this.entities = Collections.singleton(
				new JaxbImporter<>(JaxbTestEntity.class).importFile(this.dataDir.findFile("JaxbTestEntity.xml")));
	}

	@Override
	public int getOrder() {
		// We want to run as late as possible
		return ORDER;
	}

}
