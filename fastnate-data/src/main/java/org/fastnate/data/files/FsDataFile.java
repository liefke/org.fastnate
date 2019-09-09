package org.fastnate.data.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a data file from the file system.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class FsDataFile implements DataFile {

	/** The represented file from the file system. */
	@Getter
	private final File file;

	@Override
	public DataFolder getFolder() {
		final File parentFile = this.file.getParentFile();
		if (parentFile == null || !parentFile.isDirectory()) {
			return null;
		}
		return new FsDataFolder(parentFile);
	}

	@Override
	public String getName() {
		return this.file.getName();
	}

	@Override
	public InputStream open() throws IOException {
		return new FileInputStream(this.file);
	}

}
