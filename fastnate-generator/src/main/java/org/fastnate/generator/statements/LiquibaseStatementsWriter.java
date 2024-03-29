package org.fastnate.generator.statements;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.fastnate.generator.RelativeDate;
import org.fastnate.generator.RelativeDate.Precision;
import org.fastnate.generator.RelativeDate.ReferenceDate;
import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.dialect.H2Dialect;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Generates statements for the Liquibase changelog file.
 *
 * @see <a href="https://www.liquibase.org/">Liquibase on the web</a>
 *
 * @author Tobias Liefke
 */
@Getter
public class LiquibaseStatementsWriter extends AbstractStatementsWriter {

	/**
	 * {@link GeneratorContext#getSettings() Settings key} for the generated XML file, if not given in the constructor.
	 */
	public static final String OUTPUT_FILE_KEY = "fastnate.liquibase.file";

	/**
	 * {@link GeneratorContext#getSettings() Settings key} for the generated liquibase version, if not given in the
	 * constructor.
	 */
	public static final String VERSION_KEY = "fastnate.liquibase.version";

	private static final Precision[] DATE_PRECISIONS = { RelativeDate.YEARS, RelativeDate.DAYS, RelativeDate.HOURS,
			RelativeDate.MINUTES };

	private static final FastDateFormat ISO_DATETIMESECONDS_FORMAT = FastDateFormat
			.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS");

	/** The current database dialect (in case any plain SQL statement needs to be written). */
	private final GeneratorDialect dialect;

	/** The receiver of our XML elements. */
	private final XMLStreamWriter writer;

	/** Indicates if our liquibase version supports relative dates. */
	private final boolean relativeDatesSupported;

	/** The outputstream to close, if this writer is closed. */
	@Getter(AccessLevel.NONE)
	private OutputStream outputStream;

	/** The ID of the next created change set. */
	private String changeSetId = "change";

	/** The author of the next created change set. */
	private String changeSetAuthour = "Unknown";

	/** The comment of the next created change set. */
	private String changeSetComment = "Created by Fastnate";

	/** Indicates if we are currently in a changeSet. */
	private boolean changeSetStarted;

	/**
	 * Creates a new writer which generates an XML file with "UTF-8" encoding.
	 *
	 * @param context
	 *            contains all the settings that we need to build the file
	 * @throws XMLStreamException
	 *             if already the root element could not be created
	 * @throws FileNotFoundException
	 *             if the parent directory does not exist
	 */
	public LiquibaseStatementsWriter(final GeneratorContext context) throws XMLStreamException, FileNotFoundException {
		this(context.getDialect(),
				new BufferedOutputStream(
						new FileOutputStream(context.getSettings().getProperty(OUTPUT_FILE_KEY, "changelog.xml"))),
				// "1.9" was not able to write "valueComputed", so we use "2.0" as default
				context.getSettings().getProperty(VERSION_KEY, "2.0"));
	}

	/**
	 * Creates a new writer which generates an XML file with "UTF-8" encoding.
	 *
	 * @param dialect
	 *            the database dialect for any plain SQL statement
	 * @param outputStream
	 *            the target stream, will be close when this wirter is closed
	 * @param version
	 *            the liquibase version - this is only for the referenced schema and won't change anything else
	 * @throws XMLStreamException
	 *             if already the root element could not be created
	 */
	public LiquibaseStatementsWriter(final GeneratorDialect dialect, final OutputStream outputStream,
			final String version) throws XMLStreamException {
		this(dialect, XMLOutputFactory.newFactory().createXMLStreamWriter(outputStream), version);

		// Remember the outputStream to close it at the end
		this.outputStream = outputStream;
	}

	/**
	 * Creates a new writer.
	 *
	 * @param dialect
	 *            the database dialect for any plain SQL statement
	 * @param writer
	 *            the stream writer for generating the XML
	 * @param version
	 *            the liquibase version - this is only for the referenced schema and won't change anything else
	 * @throws XMLStreamException
	 *             if already the root element could not be created
	 */
	public LiquibaseStatementsWriter(final GeneratorDialect dialect, final XMLStreamWriter writer, final String version)
			throws XMLStreamException {
		this.dialect = dialect;
		this.writer = writer;
		writer.writeStartDocument();
		writer.writeCharacters("\n");
		writer.writeStartElement("databaseChangeLog");
		String namespace = "http://www.liquibase.org/xml/ns/dbchangelog";
		if (version.matches("1\\..*")) {
			namespace += "/" + version;
		}
		writer.writeDefaultNamespace(namespace);
		writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
				namespace + " http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-" + version + ".xsd");
		this.relativeDatesSupported = !version.matches("[1-3]\\..*");
	}

	/**
	 * Creates a new writer which generates an XML file with "UTF-8" encoding and use the H2 dialect for any plain
	 * statement.
	 *
	 * @param outputStream
	 *            the target stream, will be close when this wirter is closed
	 * @param version
	 *            the liquibase version - this is only for the referenced schema and won't change anything else
	 * @throws XMLStreamException
	 *             if already the root element could not be created
	 */
	public LiquibaseStatementsWriter(final OutputStream outputStream, final String version) throws XMLStreamException {
		this(new H2Dialect(), outputStream, version);
	}

	/**
	 * Creates a new writer which uses H2 for any plain statement.
	 *
	 * @param writer
	 *            the stream writer for generating the XML
	 * @param version
	 *            the liquibase version - this is only for the referenced schema and won't change anything else
	 * @throws XMLStreamException
	 *             if already the root element could not be created
	 */
	public LiquibaseStatementsWriter(final XMLStreamWriter writer, final String version) throws XMLStreamException {
		this(new H2Dialect(), writer, version);
	}

	/**
	 * Writes the remaining elements.
	 *
	 * This will not close the output stream, you will need to close it manually.
	 */
	@Override
	public void close() throws IOException {
		try {
			if (this.changeSetStarted) {
				this.writer.writeCharacters("\n\t");
				this.writer.writeEndElement();
				this.changeSetStarted = false;
			}

			this.writer.writeCharacters("\n");
			this.writer.writeEndElement();
			this.writer.writeEndDocument();
			this.writer.close();

			if (this.outputStream != null) {
				this.outputStream.close();
			}
		} catch (final XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private void ensureChangeSetStarted() throws XMLStreamException {
		if (!this.changeSetStarted) {
			this.writer.writeCharacters("\n\t");
			this.writer.writeStartElement("changeSet");
			this.writer.writeAttribute("id", this.changeSetId);
			this.writer.writeAttribute("author", this.changeSetAuthour);
			if (this.changeSetComment != null) {
				this.writer.writeCharacters("\n\t\t");
				this.writer.writeStartElement("comment");
				this.writer.writeCharacters(this.changeSetComment);
				this.writer.writeEndElement();
			}
			this.changeSetStarted = true;
		}
	}

	@Override
	public void flush() throws IOException {
		try {
			this.writer.flush();
		} catch (final XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Closes the current change set (if any was started) and sets the infos of the next change set.
	 *
	 * @param id
	 *            the ID of the next change set
	 * @param author
	 *            the author of the next change set
	 * @param comment
	 *            the (optional) comment of the next change set
	 * @throws XMLStreamException
	 *             if there is an error during closing the current change set
	 */
	public void startNextChangeSet(final String id, final String author, final String comment)
			throws XMLStreamException {
		if (this.changeSetStarted) {
			this.writer.writeCharacters("\n\t");
			this.writer.writeEndElement();
			this.changeSetStarted = false;
		}
		this.changeSetId = id;
		this.changeSetAuthour = author;
		this.changeSetComment = comment;
	}

	private void writeColumnExpression(final ColumnExpression expression) throws XMLStreamException {
		if (expression instanceof PrimitiveColumnExpression) {
			writePrimitiveExpression((PrimitiveColumnExpression<?>) expression);
		} else if (expression instanceof SequenceValueExpression) {
			writeSequenceExpression((SequenceValueExpression) expression);
		} else {
			this.writer.writeAttribute("valueComputed", expression.toSql());
		}
	}

	@Override
	public void writeComment(final String comment) throws IOException {
		try {
			if (this.changeSetStarted) {
				this.writer.writeCharacters("\n\t\t");
			} else {
				this.writer.writeCharacters("\n\t");
			}
			this.writer.writeComment(' ' + comment + ' ');
		} catch (final XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private void writeDateExpression(final ColumnExpression expression, final Date value, final Date databaseValue)
			throws XMLStreamException {
		if (value instanceof RelativeDate || value instanceof ReferenceDate) {
			if (this.relativeDatesSupported) {
				if (value == RelativeDate.NOW) {
					this.writer.writeAttribute("valueDate", "now");
					return;
				}
				if (value == RelativeDate.TODAY) {
					this.writer.writeAttribute("valueDate", "today");
					return;
				}
				if (value instanceof RelativeDate) {
					writeRelativeDateExpression(expression, value);
					return;
				}
			}
			this.writer.writeAttribute("valueComputed", expression.toSql());
		} else if (databaseValue instanceof java.sql.Time) {
			this.writer.writeAttribute("valueDate",
					DateFormatUtils.ISO_8601_EXTENDED_TIME_FORMAT.format(databaseValue));
		} else if (databaseValue instanceof java.sql.Date) {
			this.writer.writeAttribute("valueDate",
					DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(databaseValue));
		} else if (value.getTime() % RelativeDate.SECONDS.getMillis() == 0) {
			this.writer.writeAttribute("valueDate",
					DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(databaseValue));
		} else {
			this.writer.writeAttribute("valueDate", ISO_DATETIMESECONDS_FORMAT.format(databaseValue));
		}
	}

	@Override
	public void writePlainStatement(final GeneratorDialect currentDialect, final String sql) throws IOException {
		try {
			ensureChangeSetStarted();
			this.writer.writeCharacters("\n\t\t");
			this.writer.writeStartElement("sql");
			this.writer.writeCharacters(sql);
			this.writer.writeEndElement();
		} catch (final XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private void writePrimitiveExpression(final PrimitiveColumnExpression<?> expression) throws XMLStreamException {
		final Object value = expression.getValue();
		if (value != null) {
			if (value instanceof String) {
				writeString(expression, (String) value);
			} else if (value instanceof Number) {
				this.writer.writeAttribute("valueNumeric", value.toString());
			} else if (value instanceof Boolean) {
				this.writer.writeAttribute("valueNumeric", Boolean.TRUE.equals(value) ? "1" : "0");
			} else if (value instanceof Date) {
				writeDateExpression(expression, (Date) value,
						((PrimitiveColumnExpression<Date>) expression).getDatabaseValue());
			} else if (value instanceof Temporal) {
				this.writer.writeAttribute("value", value.toString());
			} else if (value instanceof Duration) {
				this.writer.writeAttribute("valueNumeric", Long.toString(((Duration) value).toNanos()));
			} else {
				this.writer.writeAttribute("valueComputed", expression.toSql());
			}
		}
	}

	private void writeRelativeDateExpression(final ColumnExpression expression, final Date value)
			throws XMLStreamException {
		final RelativeDate date = (RelativeDate) value;
		if (date.getReferenceDate() == RelativeDate.NOW) {
			final Precision minimumPrecision = date.getPrecision();
			final Precision precision = Stream.of(DATE_PRECISIONS)
					.filter(p -> p.getMillis() <= minimumPrecision.getMillis()).findFirst()
					.orElse(RelativeDate.MINUTES);
			this.writer.writeAttribute("valueDate", "now" + (date.getDifference() > -precision.getMillis() ? "+" : "")
					+ date.getDifference() / precision.getMillis() + precision.getUnit() + "s");
		} else if (date.getReferenceDate() == RelativeDate.TODAY) {
			this.writer.writeAttribute("valueDate",
					"today" + (date.getDifference() > -RelativeDate.DAYS.getMillis() ? "+" : "")
							+ date.getDifference() / RelativeDate.DAYS.getMillis());
		} else {
			this.writer.writeAttribute("valueComputed", expression.toSql());
		}
	}

	@Override
	public void writeSectionSeparator() throws IOException {
		try {
			this.writer.writeCharacters("\n");
		} catch (final XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private void writeSequenceExpression(final SequenceValueExpression expression) throws XMLStreamException {
		if (expression.getDifference() == 0) {
			if (expression instanceof NextSequenceValueExpression) {
				this.writer.writeAttribute("valueSequenceNext", expression.getSequence().getQualifiedName());
				return;
			}
			if (expression instanceof CurrentSequenceValueExpression) {
				this.writer.writeAttribute("valueSequenceCurrent", expression.getSequence().getQualifiedName());
				return;
			}
		}
		this.writer.writeAttribute("valueComputed", expression.toSql());
	}

	@Override
	public void writeStatement(final EntityStatement statement) throws IOException {
		if (statement instanceof InsertStatement) {
			try {
				final InsertStatement insert = (InsertStatement) statement;
				if (insert.getValues().isEmpty()) {
					writePlainStatement(null, insert.toSql());
				} else {
					ensureChangeSetStarted();

					this.writer.writeCharacters("\n\t\t");

					this.writer.writeStartElement("insert");

					writeTableStatement(insert);

					this.writer.writeCharacters("\n\t\t");
					this.writer.writeEndElement();
				}
			} catch (final XMLStreamException e) {
				throw new IOException(e);
			}
		} else if (statement instanceof UpdateStatement) {
			try {
				ensureChangeSetStarted();
				final UpdateStatement update = (UpdateStatement) statement;

				this.writer.writeCharacters("\n\t\t");
				this.writer.writeStartElement("update");
				writeTableStatement(update);

				this.writer.writeCharacters("\n\t\t\t");
				this.writer.writeStartElement("where");
				this.writer.writeCharacters(update.getIdColumn().getQualifiedName());
				this.writer.writeCharacters(" = ");
				this.writer.writeCharacters(update.getIdValue().toSql());
				this.writer.writeEndElement();

				this.writer.writeCharacters("\n\t\t");
				this.writer.writeEndElement();
			} catch (final XMLStreamException e) {
				throw new IOException(e);
			}
		} else {
			writePlainStatement(null, statement.toSql());
		}
	}

	private void writeString(final ColumnExpression expression, final String value) throws XMLStreamException {
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) < ' ') {
				this.writer.writeAttribute("valueComputed", expression.toSql());
				return;
			}
		}
		this.writer.writeAttribute("value", value);
	}

	private void writeTableStatement(final AbstractTableStatement insert) throws XMLStreamException {
		if (insert.getTable().getCatalog() != null) {
			this.writer.writeAttribute("catalogName", insert.getTable().getCatalog());
		}
		if (insert.getTable().getSchema() != null) {
			this.writer.writeAttribute("schemaName", insert.getTable().getSchema());
		}
		this.writer.writeAttribute("tableName", insert.getTable().getUnquotedName());
		for (final Entry<GeneratorColumn, ColumnExpression> entry : insert.getValues().entrySet()) {
			this.writer.writeCharacters("\n\t\t\t");
			this.writer.writeEmptyElement("column");
			this.writer.writeAttribute("name", entry.getKey().getUnquotedName());
			writeColumnExpression(entry.getValue());
		}
	}

}
