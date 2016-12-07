package org.fastnate.generator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.EntityStatement;

import com.google.common.io.Closeables;

import lombok.Getter;

/**
 * Extension of {@link EntitySqlGenerator} which writes the statements into a {@link Writer} resp. {@link File}.
 *
 * @author Tobias Liefke
 */
public class WriterEntitySqlGenerator extends EntitySqlGenerator {

	/** Used to write the SQL statements. */
	@Getter
	private final Writer writer;

	/**
	 * Creates a new generator for a given writer.
	 *
	 * @param writer
	 *            the writer of the file to generate
	 */
	public WriterEntitySqlGenerator(final Writer writer) {
		this(writer, new GeneratorContext());
	}

	/**
	 * Creates a new instance of {@link WriterEntitySqlGenerator}.
	 *
	 * @param writer
	 *            the writer of the file to generate
	 * @param context
	 *            Used to keep the state of indices and to store any configuration
	 */
	public WriterEntitySqlGenerator(final Writer writer, final GeneratorContext context) {
		super(context);
		this.writer = writer;
	}

	@Override
	public void close() throws IOException {
		super.close();
		Closeables.close(this.writer, false);
	}

	@Override
	public void writeComment(final String comment) throws IOException {
		this.writer.write("/* " + comment + " */\n");
	}

	@Override
	public void writeSectionSeparator() throws IOException {
		this.writer.write('\n');
	}

	@Override
	public void writeStatement(final EntityStatement stmt) throws IOException {
		this.writer.write(getContext().getDialect().createSql(stmt));
	}

}
