package org.fastnate.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;

import org.apache.commons.lang3.StringUtils;
import org.fastnate.data.files.DataFile;
import org.fastnate.data.files.DataFolder;
import org.fastnate.data.files.FsDataFile;
import org.fastnate.data.files.FsDataFolder;
import org.fastnate.data.files.VfsDataFolder;
import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.FileStatementsWriter;
import org.fastnate.generator.statements.StatementsWriter;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.vfs.Vfs;
import org.reflections.vfs.Vfs.DefaultUrlTypes;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Main class for importing entities.
 *
 * Discovers all implementations of {@link DataProvider} and creates one big SQL file for the
 * {@link DataProvider#buildEntities() generated entities}.
 *
 * @author Andreas Penski
 * @author Tobias Liefke
 */
@Slf4j
@Getter
public class EntityImporter {

	/**
	 * The String in the SQL, if the generation was aborted.
	 *
	 * Can be used by other utilities that perform further modifications on the generated files.
	 */
	public static final String GENERATION_ABORTED_MESSAGE = "!!! GENERATION ABORTED !!!";

	/**
	 * Settings key for the folder that contains any data to import.
	 *
	 * May point to a package name, which indicates that the folder is part of the class path.
	 *
	 * This setting may be overriden by the optional first command line argument of the importer.
	 *
	 * For importing entities with generic data providers, there has to be an "entities" folder in that folder, which
	 * contains the generic data files.
	 */
	public static final String DATA_FOLDER_KEY = "fastnate.data.folder";

	/** Settings key for the generated SQL file. */
	public static final String OUTPUT_FILE_KEY = FileStatementsWriter.OUTPUT_FILE_KEY;

	/** Settings key for the encoding of the generated SQL file. */
	public static final String OUTPUT_ENCODING_KEY = FileStatementsWriter.OUTPUT_ENCODING_KEY;

	/** Settings key for a part to write into the output file before the generated content. */
	public static final String PREFIX_KEY = "fastnate.data.sql.prefix";

	/** Settings key for a part to write into the output file after the generated content. */
	public static final String POSTFIX_KEY = "fastnate.data.sql.postfix";

	/**
	 * Settings key for the fully qualified name of a class that scans and instantiates the {@link DataProvider}.
	 * Defaults to {@link DefaultDataProviderFactory}.
	 */
	public static final String FACTORY_KEY = "fastnate.data.provider.factory";

	/** Settings key for the packages to scan (separated by ';', ',', ':' or whitespaces). */
	public static final String PACKAGES_KEY = "fastnate.data.provider.packages";

	/**
	 * Settings key for the packages to scan for entity classes on startup (separated by ';', ',', ':' or whitespaces).
	 */
	public static final String ENTITY_PACKAGES_KEY = "fastnate.data.entity.packages";

	/**
	 * Settings key that indicates the type of the used {@link StatementsWriter}.
	 *
	 * Allowed values of the setting:
	 * <ul>
	 * <li>FileStatementsWriter (default)</li>
	 * <li>PostgreSqlBulkWriter</li>
	 * <li>ConnectedStatementsWriter</li>
	 * <li>LiquibaseStatementsWriter</li>
	 * <li>any fully qualified class which has a constructor that accepts a {@link GeneratorContext}</li>
	 * </ul>
	 */
	public static final String STATEMENTS_WRITER_KEY = "fastnate.data.statements.writer";

	private static DataFolder findDataFolder(final GeneratorContext context) {
		final String dataFolderPath = context.getSettings().getProperty(DATA_FOLDER_KEY, ".");
		final File dataFolderDir = new File(dataFolderPath);
		try {
			if (dataFolderDir.isDirectory()) {
				log.info("Using directory {} as data folder", dataFolderDir.getAbsolutePath());
				// The data folder is a directory on the file system
				return new FsDataFolder(dataFolderDir);
			}
			if (dataFolderDir.isFile()
					&& (dataFolderDir.getName().endsWith(".jar") || dataFolderDir.getName().endsWith(".zip"))) {
				log.info("Using ZIP file {} as data folder", dataFolderDir.getAbsolutePath());
				// The data folder is a JAR file
				return new VfsDataFolder(
						Collections.singletonList(Vfs.fromURL(dataFolderDir.toURI().toURL(), DefaultUrlTypes.jarFile)));
			}
			if (dataFolderPath.matches("(?i)[-a-z0-9_]+(\\.[-a-z0-9_]+)*")) {
				// The data folder is part of the class path
				log.info("Using package \"{}\" as data folder", dataFolderPath);
				return new VfsDataFolder(
						ClasspathHelper.forClassLoader().stream().map(Vfs::fromURL).collect(Collectors.toList()))
								.getPath(dataFolderPath.replace('.', '/'));
			}
			throw new IllegalArgumentException("Data folder not found: " + dataFolderPath);
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("Can't generate URL for data folder: " + e, e);
		}
	}

	/**
	 * Starts the entity importer from the command line.
	 *
	 * Command line arguments:
	 * <ol>
	 * <li>(optional) the name or path of the generated file - defaults to "data.sql"</li>
	 * <li>(optional) the path / package of the base folder for reading input files (only used by DataProviders) -
	 * defaults to the setting of {@link #DATA_FOLDER_KEY}.
	 * </ol>
	 *
	 * You can set any other properties for {@link #EntityImporter(Properties)} or {@link GeneratorContext} with system
	 * properties.
	 *
	 * @param args
	 *            the command line arguments (see above)
	 * @throws IOException
	 *             the exception
	 */
	public static void main(final String[] args) throws IOException {
		final Properties settings = new Properties(System.getProperties());
		if (args.length > 0) {
			if (new File(args[0]).isDirectory()) {
				settings.put(DATA_FOLDER_KEY, args[0]);
				if (args.length > 1) {
					settings.put(OUTPUT_FILE_KEY, args[1]);
				}
			} else {
				settings.put(OUTPUT_FILE_KEY, args[0]);
				if (args.length > 1) {
					settings.put(DATA_FOLDER_KEY, args[1]);
				}
			}
		}
		new EntityImporter(settings).importData();
	}

	private final DataFolder dataFolder;

	private final GeneratorContext context;

	private final List<DataProvider> dataProviders = new ArrayList<>();

	/**
	 * Creates a new default instance of an EntityImporter.
	 */
	public EntityImporter() {
		this(new Properties());
	}

	/**
	 * Creates a new instance of an EntityImporter.
	 *
	 * @param context
	 *            the current generator context
	 */
	public EntityImporter(final GeneratorContext context) {
		this.context = context;
		// Determine data folder
		this.dataFolder = findDataFolder(context);

		// Scan for entity classes
		final String entityPackages = getSettings().getProperty(ENTITY_PACKAGES_KEY, "").trim();
		if (!entityPackages.isEmpty()) {
			final Reflections reflections = new Reflections((Object[]) entityPackages.split("[\\s;,:]+"));
			reflections.getTypesAnnotatedWith(Entity.class).forEach(context::getDescription);
		}

		log.info("Building all instances of {}", DataProvider.class.getSimpleName());
		String providerFactoryName = getSettings().getProperty(FACTORY_KEY, "");
		if (providerFactoryName.isEmpty()) {
			try {
				Class.forName("jakarta.inject.Inject");
				providerFactoryName = "org.fastnate.data.InjectDataProviderFactory";
			} catch (final ClassNotFoundException e) {
				providerFactoryName = DefaultDataProviderFactory.class.getName();
			}
		}
		try {
			final Class<? extends DataProviderFactory> providerFactoryClass = (Class<? extends DataProviderFactory>) Class
					.forName(providerFactoryName);
			final DataProviderFactory providerFactory = providerFactoryClass.newInstance();
			providerFactory.createDataProviders(this);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Could not find DataProviderFactory: " + providerFactoryName, e);
		} catch (final InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Could not create DataProviderFactory: " + providerFactoryName, e);
		}
	}

	/**
	 * Creates a new instance of an EntityImporter.
	 *
	 * @param settings
	 *            the settings of this importer, the data providers and the SQL generator
	 */
	public EntityImporter(final Properties settings) {
		this(new GeneratorContext(settings));
	}

	/**
	 * Adds a provider to the list of available providers.
	 *
	 * The provider will be added after the last provider with the same or a smaller order criteria.
	 *
	 * @param provider
	 *            the provider
	 *
	 */
	public void addDataProvider(final DataProvider provider) {
		addDataProvider(provider, provider.getOrder());
	}

	/**
	 * Adds a provider to the list of available providers.
	 *
	 * The provider will be added after the last provider with the same or a smaller order criteria.
	 *
	 * @param provider
	 *            the provider
	 * @param maximumOrderOfDepenendencies
	 *            the maximum {@link DataProvider#getOrder() ordering} of dependencies of the provider
	 *
	 */
	public void addDataProvider(final DataProvider provider, final int maximumOrderOfDepenendencies) {
		final int order = Math.max(maximumOrderOfDepenendencies, provider.getOrder());
		int index = this.dataProviders.size();
		while (index > 0 && this.dataProviders.get(index - 1).getOrder() > order) {
			index--;
		}
		this.dataProviders.add(index, provider);
	}

	/**
	 * Resolves the first provider that is an instance of the given class.
	 *
	 * @param providerClass
	 *            the provider class
	 * @return the provider with that class or {@code null} if no such provider exists
	 */
	public <P extends DataProvider> P findDataProvider(final Class<P> providerClass) {
		return providerClass
				.cast(this.dataProviders.stream().filter(providerClass::isInstance).findFirst().orElse(null));
	}

	private Charset getEncoding() {
		return Charset.forName(getSettings().getProperty(OUTPUT_ENCODING_KEY, "UTF-8"));
	}

	/**
	 * The {@link GeneratorContext#getSettings() settings} of the {@link #getContext() context}.
	 *
	 * @return settings used by this importer
	 */
	public Properties getSettings() {
		return this.context.getSettings();
	}

	/**
	 * Imports the data and creates the SQL.
	 *
	 * Depending on the setting {@link #STATEMENTS_WRITER_KEY}, this will either create a file or execute the SQL
	 * against a connection.
	 *
	 * @throws IOException
	 *             if one of the data importers or the file writer throws one
	 */
	public void importData() throws IOException {
		String statementsWriter = getSettings().getProperty(STATEMENTS_WRITER_KEY,
				FileStatementsWriter.class.getSimpleName());
		if (statementsWriter.indexOf('.') < 0) {
			statementsWriter = "org.fastnate.generator.statements." + statementsWriter;
		}
		try {
			log.info("Using {} as statements writer", statementsWriter);

			final Class<StatementsWriter> statementsWriterClass = (Class<StatementsWriter>) Class
					.forName(statementsWriter);
			final StatementsWriter writer = statementsWriterClass.getConstructor(GeneratorContext.class)
					.newInstance(this.context);
			try (EntitySqlGenerator generator = new EntitySqlGenerator(this.context, writer)) {
				if (writer instanceof ConnectedStatementsWriter) {
					importData(generator, ((ConnectedStatementsWriter) writer).getConnection());
				} else {
					importData(generator);
				}
			}
		} catch (final SQLException e) {
			throw new IOException(e);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Unknown statements writer: " + statementsWriter, e);
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException(
					"Statements writer needs a (GeneratorContext) constructor: " + statementsWriter, e);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException e) {
			throw new IllegalArgumentException("Could not build statements writer: " + statementsWriter, e);
		}
	}

	/**
	 * Asks the data providers to generate their entities and writes the SQL to a database connection at the end.
	 *
	 * @param connection
	 *            the target connection for the SQL statements
	 *
	 * @throws SQLException
	 *             if the connection throws an exception
	 * @throws IOException
	 *             if the generator throws an exception
	 */
	public void importData(final Connection connection) throws IOException, SQLException {
		try (EntitySqlGenerator generator = new EntitySqlGenerator(this.context, connection)) {
			importData(generator, connection);
		} catch (final IOException e) {
			if (e.getCause() instanceof SQLException) {
				throw (SQLException) e.getCause();
			}
			throw e;
		}
	}

	/**
	 * Asks the data providers to generate their entities and writes the SQL afterwards.
	 *
	 * @param generator
	 *            the current entity SQL generator
	 *
	 * @throws IOException
	 *             if the generator throws an exception
	 */
	public void importData(final EntitySqlGenerator generator) throws IOException {
		try {
			final String dialect = this.context.getDialect().getClass().getSimpleName();
			log.info("Using {} for SQL generation.", dialect);
			for (final DataProvider provider : this.dataProviders) {
				provider.buildEntities();
			}

			generator.writeComment("Generated by Fastnate EntityImporter for " + dialect);

			writePropertyPart(generator, PREFIX_KEY);

			for (final DataProvider provider : this.dataProviders) {
				generator.writeSectionSeparator();
				generator.writeComment("Data from " + provider.getClass().getSimpleName());
				provider.writeEntities(generator);
				log.info("Generated SQL for {}", provider.getClass());
			}

			generator.writeAlignmentStatements();

			writePropertyPart(generator, POSTFIX_KEY);

			// CHECKSTYLE OFF: IllegalCatch
		} catch (final IOException | RuntimeException | Error e) {
			// CHECKSTYLE ON

			// Write stacktrace as a comment to the result file
			generator.writeSectionSeparator();
			final StringWriter buffer = new StringWriter();
			buffer.write('\n' + GENERATION_ABORTED_MESSAGE + '\n');
			e.printStackTrace(new PrintWriter(buffer, true));
			generator.writeComment(buffer.toString());
			throw e;
		}
	}

	private void importData(final EntitySqlGenerator generator, final Connection connection)
			throws SQLException, IOException {
		final boolean transation = connection.getAutoCommit() && this.context.getDialect().isFastInTransaction();
		if (transation) {
			connection.setAutoCommit(false);
		}
		try {
			importData(generator);
			if (transation) {
				connection.commit();
			}
			// CHECKSTYLE OFF: IllegalCatch
		} catch (final RuntimeException | IOException | SQLException e) {
			// CHECKSTYLE ON
			if (transation) {
				connection.rollback();
			}
			throw e;
		} finally {
			if (transation) {
				connection.setAutoCommit(true);
			}
		}
	}

	/**
	 * Imports the data and creates the given SQL file.
	 *
	 * @param targetFile
	 *            the SQL file to generate
	 * @throws IOException
	 *             if one of the data importers or the file writer throws one
	 */
	public void importData(final File targetFile) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(targetFile), getEncoding()))) {
			importData(writer);
			log.info("'{}' generated.", targetFile.getAbsolutePath());
		}
	}

	/**
	 * Asks the data providers to generate their entities and writes the SQL to a file at the end.
	 *
	 * @param writer
	 *            the target writer for the SQL statements
	 *
	 * @throws IOException
	 *             if the generator or writer throws an exception
	 */
	public void importData(final Writer writer) throws IOException {
		try (EntitySqlGenerator generator = new EntitySqlGenerator(this.context, writer)) {
			importData(generator);
		}
	}

	/**
	 * Writes a section from a property to the writer of the SQL generator.
	 *
	 * @param generator
	 *            the current generator
	 * @param property
	 *            name of the property to write, contains either a list of file names (separated by ',' and ending with
	 *            ".sql"), or an SQL statement
	 * @throws IOException
	 *             if the writer or reader throws one
	 */
	private void writePropertyPart(final EntitySqlGenerator generator, final String property) throws IOException {
		if (!(generator.getWriter() instanceof FileStatementsWriter)) {
			return;
		}
		final Writer writer = ((FileStatementsWriter) generator.getWriter()).getWriter();
		final String propertyValue = StringUtils.trimToNull(getSettings().getProperty(property));
		if (propertyValue != null) {
			generator.writeSectionSeparator();
			if (propertyValue.endsWith(".sql")) {
				final String[] fileNames = propertyValue.split("[\\n\\" + File.pathSeparatorChar + ",;]+");
				for (final String fileName : fileNames) {
					final File sqlFile = new File(fileName);
					final DataFile sqlDataFile;
					if (sqlFile.isAbsolute()) {
						sqlDataFile = sqlFile.isFile() ? new FsDataFile(sqlFile) : null;
					} else {
						sqlDataFile = this.dataFolder.findFile(fileName);
					}
					if (sqlDataFile != null) {
						try (InputStreamReader input = new InputStreamReader(sqlDataFile.open(), getEncoding())) {
							generator.writeComment(fileName);
							final int bufferSize = 1024;
							final char[] buffer = new char[bufferSize];
							for (int read; (read = input.read(buffer)) > 0;) {
								writer.write(buffer, 0, read);
							}
							writer.write("\n");
						}
					} else {
						generator.writeComment("Ignored missing file: " + fileName);
					}
				}
			} else {
				generator.writeComment(property);
				writer.write(propertyValue);
				writer.write("\n");
			}
		}
	}
}
