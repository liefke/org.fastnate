package org.fastnate.data.xml;

import java.io.IOException;
import java.io.InputStream;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.fastnate.data.DataImportException;
import org.fastnate.data.files.DataFile;

import lombok.RequiredArgsConstructor;

/**
 * Imports an entity from an XML file using {@link JAXB}.
 *
 * @author Tobias Liefke
 * @param <T>
 *            the type of the imported entities
 */
@RequiredArgsConstructor
public class JaxbImporter<T> {

	/** The type of the imported entities. */
	private final Class<T> entityClass;

	/**
	 * Imports the entity from the given file.
	 *
	 * @param file
	 *            the file that contains the entity
	 * @return the entity from that file
	 * @throws IOException
	 *             if the file was not accessible
	 * @throws DataImportException
	 *             if the file content was invalid
	 */
	public T importFile(final DataFile file) throws IOException, DataImportException {
		final JAXBContext jaxb;
		try {
			jaxb = JAXBContext.newInstance(this.entityClass);
		} catch (final JAXBException e) {
			throw new DataImportException("Could not create context: " + e, e);
		}
		try (InputStream stream = file.open()) {
			return (T) jaxb.createUnmarshaller().unmarshal(stream);
		} catch (final JAXBException e) {
			throw new DataImportException("Could not parse content: " + e, file.getName(), e);
		}
	}

}
