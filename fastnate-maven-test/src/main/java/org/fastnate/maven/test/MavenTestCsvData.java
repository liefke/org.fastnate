package org.fastnate.maven.test;

import java.io.IOException;
import java.util.Collection;

import jakarta.annotation.Resource;

import org.fastnate.data.AbstractDataProvider;
import org.fastnate.data.csv.CsvDataImporter;
import org.fastnate.data.files.DataFolder;
import org.fastnate.generator.context.GeneratorContext;
import org.supercsv.prefs.CsvPreference;

import lombok.Getter;

/**
 * Test class for importing CSV files using the maven plugin.
 *
 * @author Tobias Liefke
 */
public class MavenTestCsvData extends AbstractDataProvider {

	@Resource
	private DataFolder dataFolder;

	@Resource
	private GeneratorContext context;

	@Getter
	private Collection<MavenTestEntity> entities;

	@Override
	public void buildEntities() throws IOException {
		final CsvDataImporter<MavenTestEntity> importer = new CsvDataImporter<>(
				this.context.getDescription(MavenTestEntity.class), CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
		importer.mapProperties();
		this.entities = importer.importFile(this.dataFolder.getFolder("csv").findFile("maventestentities.csv"));
	}

}
