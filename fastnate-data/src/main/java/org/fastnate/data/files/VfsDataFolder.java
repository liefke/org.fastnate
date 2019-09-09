package org.fastnate.data.files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.File;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a folder from the class path.
 *
 * Wraps a set of {@link Dir Vfs.Dir}s.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class VfsDataFolder implements DataFolder {

	/** All root directories. */
	private final List<Dir> dirs;

	/** The parent directory, {@code null} if this represents the root folder. */
	@Getter
	private final VfsDataFolder parent;

	/** The full path to this folder from {@link #dirs}, {@code null} if this represents the root folder. */
	private final String relativePath;

	/** The name of this folder, {@code null} if this represents the root folder. */
	@Getter
	private final String name;

	/**
	 * Creates a new root folder.
	 *
	 * @param dirs
	 *            the directories that represent the root of the data folder
	 */
	public VfsDataFolder(final List<Dir> dirs) {
		this(dirs, null, null, null);
	}

	@Override
	public DataFile findFile(final String fileName) {
		for (final Dir dir : this.dirs) {
			for (final File file : dir.getFiles()) {
				if (fileName.equals(file.getName()) && isFileFromFolder(file)) {
					return new VfsDataFile(this, file);
				}
			}
		}
		return null;
	}

	@Override
	public DataFolder findFolder(final String folderName) {
		return new VfsDataFolder(this.dirs, this,
				this.relativePath == null ? folderName : this.relativePath + '/' + folderName, folderName);
	}

	@Override
	public List<VfsDataFile> getFiles() {
		final List<VfsDataFile> result = new ArrayList<>();
		for (final Dir dir : this.dirs) {
			for (final File file : dir.getFiles()) {
				if (isFileFromFolder(file)) {
					result.add(new VfsDataFile(this, file));
				}
			}
		}
		Collections.sort(result, Comparator.comparing(DataFile::getName));
		return result;
	}

	@Override
	public List<VfsDataFolder> getFolders() {
		final Set<String> folders = new LinkedHashSet<>();
		final List<VfsDataFolder> result = new ArrayList<>();
		for (final Dir dir : this.dirs) {
			for (final File file : dir.getFiles()) {
				final String path = file.getRelativePath();
				final int firstChar;
				if (this.relativePath == null) {
					firstChar = 0;
				} else {
					firstChar = this.relativePath.length() + 1;
					if (path.length() <= firstChar || path.charAt(firstChar - 1) != '/'
							|| !path.startsWith(this.relativePath)) {
						continue;
					}
				}
				final int nextSlash = path.indexOf('/', firstChar);
				if (nextSlash > 0) {
					final String folderName = path.substring(firstChar, nextSlash);
					if (folders.add(folderName)) {
						result.add(new VfsDataFolder(this.dirs, this, path.substring(0, nextSlash), folderName));
					}
				}
			}
		}
		Collections.sort(result, Comparator.comparing(DataFolder::getName));
		return result;
	}

	private boolean isFileFromFolder(final File file) {
		final String filePath = file.getRelativePath();
		if (this.relativePath == null) {
			return filePath.indexOf('/') < 0;
		}
		final int slash = this.relativePath.length();
		return filePath.length() > slash + 1 && filePath.charAt(slash) == '/' && filePath.startsWith(this.relativePath)
				&& filePath.indexOf('/', slash + 1) < 0;
	}

}
