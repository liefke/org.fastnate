package org.fastnate.data.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.supercsv.comment.CommentMatches;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for reading a csv file into an object. Useful for constructing arbitrary object types - not only entities.
 *
 * @author Andreas Penski
 * @param <R>
 *            the row type
 */
@Slf4j
public abstract class AbstractCsvReader<R> {

	private static boolean isNotEmpty(final List<String> values) {
		return values.size() > 1 || values.size() == 1 && StringUtils.isNotBlank(values.get(0));
	}

	private final List<File> importFiles;

	/**
	 * Creates a new instance of {@link AbstractCsvReader}.
	 *
	 * @param importPath
	 *            the path to a file or to a directory that contains *.csv files
	 */
	public AbstractCsvReader(final File importPath) {
		this.importFiles = new ArrayList<>();
		if (!importPath.exists()) {
			throw new IllegalArgumentException("Path not found: " + importPath.getAbsolutePath());
		}
		if (importPath.isDirectory()) {
			this.importFiles.addAll(Arrays.asList(importPath.listFiles((FilenameFilter) new SuffixFileFilter(".csv"))));
		} else {
			this.importFiles.add(importPath);
		}
	}

	/**
	 * Builds one or more entities from the given row.
	 *
	 * @param row
	 *            contains the mapping from the header names to the current row data
	 * @return the list of entities from that row
	 */
	protected abstract Collection<? extends R> createEntities(Map<String, String> row);

	/**
	 * Defines the default encoding for CSV files, if it can't be determined from the BOM.
	 *
	 * @return the default encoding
	 */
	protected Charset getDefaultEncoding() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * Opens a CSV file.
	 *
	 * If the given file ends with "gz", then the file is decompressed before using a {@link GZIPInputStream}.
	 *
	 * @param importFile
	 *            the csv file
	 * @return a list reader
	 * @throws IOException
	 *             on io exception
	 */
	@SuppressWarnings("resource")
	protected CsvListReader openCsvListReader(final File importFile) throws IOException {
		// Open file
		InputStream fileStream = new FileInputStream(importFile);

		// Check for compressed file
		if (importFile.getName().toLowerCase().endsWith(".gz")) {
			fileStream = new GZIPInputStream(fileStream);
		}

		// Guess the encoding
		final BOMInputStream inputStream = new BOMInputStream(fileStream, false, ByteOrderMark.UTF_8,
				ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
		final String charset;
		if (inputStream.hasBOM()) {
			charset = inputStream.getBOMCharsetName();
			log.info("BOM detected. Using {} as encoding", charset);
		} else {
			charset = getDefaultEncoding().toString();
			log.info("No BOM detected. Assuming {} as encoding", charset);
		}
		final Reader reader = new InputStreamReader(inputStream, charset);
		return new CsvListReader(reader, new CsvPreference.Builder(CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE)
				.skipComments(new CommentMatches("(//|/\\*|#|;).*")).build());
	}

	/**
	 * Reads the import files.
	 *
	 * @return collection of constructed entities
	 * @throws IOException
	 *             on error
	 */
	protected Collection<R> readImportFiles() throws IOException {
		final Collection<R> entities = new ArrayList<>();
		for (final File importFile : this.importFiles) {
			log.info("Reading entities from {}...", importFile);

			try (CsvListReader csvList = openCsvListReader(importFile)) {
				final String[] header = csvList.getHeader(true);
				if (ArrayUtils.isEmpty(header)) {
					log.error("Ignoring {}, as no header was found", importFile);
					continue;
				}
				final Map<String, Integer> columnNames = new HashMap<>();
				for (int i = 0; i < header.length; i++) {
					if (header[i] != null) {
						columnNames.put(header[i], i);
					}
				}
				for (List<String> values; (values = csvList.read()) != null;) {
					if (isNotEmpty(values)) {
						final Map<String, String> row = new HashMap<>();
						for (final Map.Entry<String, Integer> column : columnNames.entrySet()) {
							if (column.getValue() < values.size()) {
								row.put(column.getKey(), values.get(column.getValue()));
							}
						}
						entities.addAll(createEntities(row));
					}
				}
			}
		}
		return entities;
	}
}
