package org.fastnate.data.files;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.fastnate.data.DataProvider;

/**
 * Represents a folder from the file system or the classpath that contains data to import with a {@link DataProvider}.
 *
 * @author Tobias Liefke
 */
public interface DataFolder {

	/**
	 * Finds a file in this folder.
	 *
	 * @param name
	 *            the name of the file
	 *
	 * @return the file or {@code null} if no file with such a name exists
	 */
	default DataFile findFile(final String name) {
		return getFiles().stream().filter(file -> name.equals(file.getName())).findFirst().orElse(null);
	}

	/**
	 * Finds a sub folder in this folder.
	 *
	 * @param name
	 *            the name of the sub folder
	 *
	 * @return the sub folder or {@code null} if no folder with such a name exists
	 */
	default DataFolder findFolder(final String name) {
		return getFolders().stream().filter(folder -> name.equals(folder.getName())).findFirst().orElse(null);
	}

	/**
	 * Finds all files in this folder and in all sub folders and handles them with the given consumer.
	 *
	 * @param consumer
	 *            handles each of the files
	 * @throws IOException
	 *             if one of the files was not accessible
	 */
	default void forAllFiles(final BiConsumer<DataFolder, DataFile> consumer) throws IOException {
		for (final DataFile file : getFiles()) {
			try (InputStream in = file.open()) {
				consumer.accept(this, file);
			}
		}
		for (final DataFolder folder : getFolders()) {
			folder.forAllFiles(consumer);
		}
	}

	/**
	 * Finds all files in this folder and in all sub folders and handles them with the given consumer.
	 *
	 * @param consumer
	 *            handles each of the files
	 * @throws IOException
	 *             if one of the files was not accessible
	 */
	default void forAllFiles(final Consumer<DataFile> consumer) throws IOException {
		for (final DataFile file : getFiles()) {
			try (InputStream in = file.open()) {
				consumer.accept(file);
			}
		}
		for (final DataFolder folder : getFolders()) {
			folder.forAllFiles(consumer);
		}
	}

	/**
	 * Finds all files inside this folder.
	 *
	 * Will not return a file from a {@link #getFolders() sub folder}
	 *
	 * @return the files in this folder
	 */
	List<? extends DataFile> getFiles();

	/**
	 * Finds a sub folder in this folder.
	 *
	 * @param name
	 *            the name of the sub folder
	 *
	 * @return the sub folder or an empty folder, if no such folder exists
	 */
	default DataFolder getFolder(final String name) {
		final DataFolder folder = findFolder(name);
		if (folder != null) {
			return folder;
		}
		return new DataFolder() {

			@Override
			public DataFile findFile(final String name) {
				return null;
			}

			@Override
			public DataFolder findFolder(final String name) {
				return null;
			}

			@Override
			public List<? extends DataFile> getFiles() {
				return Collections.emptyList();
			}

			@Override
			public List<? extends DataFolder> getFolders() {
				return Collections.emptyList();
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public DataFolder getParent() {
				return DataFolder.this;
			}
		};
	}

	/**
	 * Finds the all sub folders inside this folder.
	 *
	 * @return the direct children of this folder
	 */
	List<? extends DataFolder> getFolders();

	/**
	 * The name of this folder.
	 *
	 * @return the name of this folder, empty or {@code null} for the root folder
	 */
	String getName();

	/**
	 * The parent folder of this folder.
	 *
	 * @return the parent or {@code null} if this folder is the root folder
	 */
	DataFolder getParent();

	/**
	 * Splits the given path by '/' and returns the folder according to the single path elements.
	 * 
	 * @param path
	 *            the relative path to the folder from this folder
	 * @return the folder for the given path
	 */
	default DataFolder getPath(final String path) {
		DataFolder result = this;
		for (final String pathElement : path.split("/")) {
			result = result.getFolder(pathElement);
		}
		return result;
	}

}
