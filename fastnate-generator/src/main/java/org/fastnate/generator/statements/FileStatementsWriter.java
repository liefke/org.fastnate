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
		this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding)));
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
