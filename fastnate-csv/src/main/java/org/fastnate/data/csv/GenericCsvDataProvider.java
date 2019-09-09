package org.fastnate.data.csv;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.fastnate.data.DataImportException;
import org.fastnate.data.EntityImporter;
import org.fastnate.data.files.DataFile;
import org.fastnate.data.files.GenericDataProvider;
import org.fastnate.generator.context.EntityClass;

/**
 * Imports all entities from all CSV files in {@link EntityImporter#DATA_FOLDER_KEY $dataFolder/entities}.
 *
 * The CSV file needs either an entity name or needs to be located inside a directory with an entity name.
 *
 * @author Tobias Liefke
 */
public class GenericCsvDataProvider extends GenericDataProvider {

	private final Map<EntityClass<?>, CsvDataImporter<?>> importers = new HashMap<>();

	/**
	 * Creates the importer for a specific entity type.
	 * 
	 * @param entityClass
	 *            the description of the entity type
	 * @return the import for that type
	 */
	private <E> CsvDataImporter<E> createImporter(final EntityClass<E> entityClass) {
		final CsvDataImporter<E> importer = new CsvDataImporter<>(entityClass, getEntityRegistration());
		importer.mapProperties();
		importer.setIgnoreMissingColumns(true);
		return importer;
	}

	@Override
	protected Collection<?> importFile(final DataFile importFile) throws DataImportException, IOException {
		final EntityClass<?> entityClass = findEntityClass(importFile);
		if (entityClass == null) {
			throw new DataImportException("Could not determine entity type for " + importFile, importFile.getName());
		}
		return this.importers.computeIfAbsent(entityClass, this::createImporter).importFile(importFile);
	}

	@Override
	protected boolean isImportFile(final DataFile file) {
		return file.getName().endsWith(".csv");
	}

}
