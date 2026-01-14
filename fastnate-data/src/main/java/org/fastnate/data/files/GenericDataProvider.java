package org.fastnate.data.files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.Resource;

import org.fastnate.data.DataImportException;
import org.fastnate.data.DataProvider;
import org.fastnate.data.EntityImporter;
import org.fastnate.data.EntityRegistration;
import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Imports all entities from all files in {@link EntityImporter#DATA_FOLDER_KEY $dataFolder/entities} that match a
 * specific pattern.
 *
 * The subclasses define the pattern (and type) of the imported files.
 *
 * @author Tobias Liefke
 */
@Slf4j
public abstract class GenericDataProvider implements DataProvider {

	/** The folder in the {@link #dataFolder} that contains the entity files. */
	private static final String ENTITIES_FOLDER = "entities";

	/** The context of the current generator, with the description of the entity classes and settings. */
	@Resource
	@Getter(AccessLevel.PROTECTED)
	private GeneratorContext context;

	/** Contains all already discovered entities with their unique property. */
	@Resource
	@Getter(AccessLevel.PROTECTED)
	private EntityRegistration entityRegistration;

	/**
	 * The data folder defined for the entity importer.
	 *
	 * The generic files are part of the {@link #ENTITIES_FOLDER} inside of this folder.
	 */
	@Resource
	private DataFolder dataFolder;

	/** The entities as returned from the importers, to prevent useless allocation of another list. */
	private final List<Collection<?>> entities = new ArrayList<>();

	@Override
	public void buildEntities() throws IOException {
		// Find all files and try to read them
		this.dataFolder.getFolder(ENTITIES_FOLDER).forAllFiles(this::readImportFile);
	}

	/**
	 * Tries to guess the class of the imported entities from the file or directory name.
	 *
	 * If the file name matches an entity name, that entity class is returned, otherwise the directory tree is walked up
	 * until the root is found or an entity name is found.
	 *
	 * @param importFile
	 *            the file that is imported
	 * @return the class according to the file or directory name
	 */
	protected EntityClass<?> findEntityClass(final DataFile importFile) {
		// Try to use the file name
		final int dot = importFile.getName().indexOf('.');
		if (dot > 0) {
			final String entityName = importFile.getName().substring(0, dot);
			final EntityClass<?> entityClass = this.context.getDescriptionsByName().get(entityName);
			if (entityClass != null) {
				return entityClass;
			}
		}

		// Try to use one of the directory names
		for (DataFolder folder = importFile.getFolder(); folder != null; folder = folder.getParent()) {
			final String entityName = folder.getName();
			final EntityClass<?> entityClass = this.context.getDescriptionsByName().get(entityName);
			if (entityClass != null) {
				return entityClass;
			}
		}
		return null;
	}

	/**
	 * Tries to import the given file.
	 *
	 * @param importFile
	 *            the file that contains the entities
	 *
	 * @return the imported entities or {@code null} if the file was not imported
	 * @throws IOException
	 *             if the file was not accessible
	 * @throws DataImportException
	 *             if the file content was invalid
	 */
	protected abstract Collection<?> importFile(DataFile importFile) throws DataImportException, IOException;

	/**
	 * Indicates that the given file is imported.
	 *
	 * Most implementations will check the suffix of the file.
	 *
	 * @param file
	 *            the file to check
	 * @return {@code true} if that file needs to be imported by this data provider
	 */
	protected abstract boolean isImportFile(DataFile file);

	private void readImportFile(final DataFile file) {
		if (isImportFile(file)) {
			try {
				log.info("Reading entities from {}...", file.getName());
				final Collection<?> importedEntities = importFile(file);
				if (importedEntities != null && importedEntities.size() > 0) {
					this.entities.add(importedEntities);
				}
			} catch (final IOException e) {
				throw new DataImportException(e.getMessage(), file.getName(), e);
			}
		}
	}

	@Override
	public void writeEntities(final EntitySqlGenerator sqlGenerator) throws IOException {
		for (final Collection<?> subset : this.entities) {
			sqlGenerator.write(subset);
		}
	}

}
