package org.fastnate.data.xml;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.fastnate.data.DataImportException;
import org.fastnate.data.EntityImporter;
import org.fastnate.data.files.DataFile;
import org.fastnate.data.files.GenericDataProvider;
import org.fastnate.generator.context.EntityClass;

/**
 * Imports all entities from all XML files in {@link EntityImporter#DATA_FOLDER_KEY $dataFolder/entities}.
 *
 * If the XML file has an entity name or is inside a directory with an entity name, and that entity class is annotated
 * with {@link XmlRootElement}, the {@link JaxbImporter} is used, otherwise the {@link XmlDataImporter} is used.
 *
 * @author Tobias Liefke
 */
public class GenericXmlDataProvider extends GenericDataProvider {

	@Override
	protected Collection<?> importFile(final DataFile importFile) throws DataImportException, IOException {
		// First try to find entity class to check if we need to use the JaxbImporter
		final EntityClass<?> entityClass = findEntityClass(importFile);
		if (entityClass != null && entityClass.getEntityClass().isAnnotationPresent(XmlRootElement.class)) {
			return Collections.singleton(new JaxbImporter<>(entityClass.getEntityClass()).importFile(importFile));
		}

		return new XmlDataImporter(getContext(), getEntityRegistration()).importFile(importFile);
	}

	@Override
	protected boolean isImportFile(final DataFile file) {
		return file.getName().endsWith(".xml");
	}

}
