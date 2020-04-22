package org.fastnate.data.files;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a data file from an URL.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class UrlDataFile implements DataFile {

	/** The parent folder, if any. */
	@Getter
	private final DataFolder folder;

	/** The represented file as URL. */
	@Getter
	private final URL url;

	/**
	 * Creates a new file from an URL without information about the parent folder.
	 * 
	 * @param url
	 *            the URL of the file we represent
	 */
	public UrlDataFile(final URL url) {
		this(null, url);
	}

	@Override
	public String getName() {
		return this.url.getPath().replaceFirst("^.*\\/", "");
	}

	@Override
	public InputStream open() throws IOException {
		return this.url.openStream();
	}

}
