package org.fastnate.data.files;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a folder from the file system.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class FsDataFolder implements DataFolder {

	/** The directory from the filesystem */
	@Getter
	private final File folder;

	@Override
	public DataFile findFile(final String name) {
		final File file = new File(this.folder, name);
		if (file.isFile()) {
			return new FsDataFile(file);
		}
		return null;
	}

	@Override
	public DataFolder findFolder(final String name) {
		final File dir = new File(this.folder, name);
		if (dir.isDirectory()) {
			return new FsDataFolder(dir);
		}
		return null;
	}

	@Override
	public List<FsDataFile> getFiles() {
		return Stream.of(this.folder.listFiles(file -> file.isFile())).sorted(Comparator.comparing(File::getName))
				.map(FsDataFile::new).collect(Collectors.toList());
	}

	@Override
	public List<FsDataFolder> getFolders() {
		return Stream.of(this.folder.listFiles(file -> file.isDirectory())).sorted(Comparator.comparing(File::getName))
				.map(FsDataFolder::new).collect(Collectors.toList());
	}

	@Override
	public String getName() {
		return this.folder.getName();
	}

	@Override
	public DataFolder getParent() {
		final File parentFile = this.folder.getParentFile();
		if (parentFile == null || !parentFile.isDirectory()) {
			return null;
		}
		return new FsDataFolder(parentFile);
	}

}
