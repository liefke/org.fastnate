package org.fastnate.generator.statements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.dialect.GeneratorDialect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link StatementsWriter} which writes the statements into a {@link Writer} resp. {@link File}.
 *
 * @author Tobias Liefke
 */
@Slf4j
@RequiredArgsConstructor
public class FileStatementsWriter extends AbstractStatementsWriter {

	/**
	 * {@link GeneratorContext#getSettings() Settings key} for the generated SQL file, if not given in the constructor.
	 */
	public static final String OUTPUT_FILE_KEY = "fastnate.data.sql.output.file";

	/**
	 * {@link GeneratorContext#getSettings() Settings key} for the encoding of the generated SQL file, if not given in
	 * the constructor.
	 */
	public static final String OUTPUT_ENCODING_KEY = "fastnate.data.sql.output.encoding";

	/**
	 * Ensures, that the parent directory of the given output file exists.
	 * 
	 * @param outputFile
	 *            the output file
	 * @return the output file (for chaining)
	 */
	protected static File ensureDirectoryExists(final File outputFile) {
		final File parentFile = outputFile.getAbsoluteFile().getParentFile();
		if (parentFile != null) {
			parentFile.mkdirs();
		}
		return outputFile;
	}

	/** Used to write the SQL statements. */
	@Getter
	private final Writer writer;

	/** The separator of the single statements, defaults to {@code ";\n"}. */
	@Getter
	@Setter
	private String statementSeparator = ";\n";

	/** The count of written statements. */
	@Getter
	private int statementsCount;

	/**
	 * Creates a new instance for a specifc file and UTF-8 encoding.
	 *
	 * @param file
	 *            the target file
	 * @throws FileNotFoundException
	 *             if the file could not be opened for writing
	 */
	public FileStatementsWriter(final File file) throws FileNotFoundException {
		this(file, StandardCharsets.UTF_8);
	}

	/**
	 * Creates a new instance for a specifc file and encoding.
	 *
	 * @param file
	 *            the target file
	 * @param encoding
	 *            the charset of the target file
	 * @throws FileNotFoundException
	 *             if the file could not be opened for writing
	 */
	public FileStatementsWriter(final File file, final Charset encoding) throws FileNotFoundException {
		this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ensureDirectoryExists(file)), encoding)));
	}

	/**
	 * Creates a new instance of {@link FileStatementsWriter}.
	 *
	 * The target file and its encoding are taken from the settings {@link #OUTPUT_FILE_KEY} and
	 * {@link #OUTPUT_ENCODING_KEY}.
	 *
	 * @param context
	 *            the context of the generation (for lookup of the properties)
	 * @throws FileNotFoundException
	 *             if the file path is not available for writing
	 */
	public FileStatementsWriter(final GeneratorContext context) throws FileNotFoundException {
		this(new File(context.getSettings().getProperty(OUTPUT_FILE_KEY, "data.sql")),
				Charset.forName(context.getSettings().getProperty(OUTPUT_ENCODING_KEY, "UTF-8")));
	}

	@Override
	public void close() throws IOException {
		this.writer.close();
		log.info("{} statements written", this.statementsCount);
	}

	@Override
	public void flush() throws IOException {
		this.writer.flush();
	}

	/**
	 * Writes a bunch of SQL statements to the file.
	 *
	 * @param statements
	 *            the SQL statements (or anything else what should be added to the file)
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void write(final String statements) throws IOException {
		this.writer.write(statements);
	}

	@Override
	public void writeComment(final String comment) throws IOException {
		this.writer.write("/* " + comment + " */\n");
	}

	@Override
	public void writePlainStatement(final GeneratorDialect dialect, final String sql) throws IOException {
		this.writer.write(sql);
		if (!sql.endsWith(this.statementSeparator)) {
			this.writer.write(this.statementSeparator);
		}
		this.statementsCount++;
	}

	@Override
	public void writeSectionSeparator() throws IOException {
		this.writer.write('\n');
	}

	@Override
	public void writeStatement(final EntityStatement statement) throws IOException {
		this.writer.write(statement.toSql());
		this.writer.write(this.statementSeparator);
		this.statementsCount++;
	}

}
