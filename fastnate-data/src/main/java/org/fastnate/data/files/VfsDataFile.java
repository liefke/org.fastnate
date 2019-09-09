package org.fastnate.data.files;

import java.io.IOException;
import java.io.InputStream;

import org.reflections.vfs.Vfs.File;

import lombok.RequiredArgsConstructor;

/**
 * Represents a file from the class path.
 *
 * Wraps a {@link File Vfs.File}.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class VfsDataFile implements DataFile {

	/** The parent directory. */
	private final VfsDataFolder parent;

	/** The file that is wrapped by this DataFile. */
	private final File file;

	@Override
	public DataFolder getFolder() {
		return this.parent;
	}

	@Override
	public String getName() {
		return this.file.getName();
	}

	@Override
	public InputStream open() throws IOException {
		return this.file.openInputStream();
	}

}
