package org.fastnate.generator.test;

import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.spi.PersistenceProvider;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.LiquibaseStatementsWriter;
import org.fastnate.generator.statements.StatementsWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.reflections.Reflections;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.InputStreamList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Test the {@link EntitySqlGenerator} framework.
 *
 * @author Tobias Liefke
 */
public class AbstractEntitySqlGeneratorTest {

	/** Liquibase resource accessor that returns only a stream from the given byte buffer. */
	@RequiredArgsConstructor
	private static final class ByteArrayResourceAccessor extends AbstractResourceAccessor {

		private final String fileName;

		private final byte[] buffer;

		@Override
		public SortedSet<String> describeLocations() {
			return new TreeSet<>(Collections.singletonList(this.fileName));
		}

		@Override
		public SortedSet<String> list(final String relativeTo, final String path, final boolean recursive,
				final boolean includeFiles, final boolean includeDirectories) throws IOException {
			final SortedSet<String> returnSet = new TreeSet<>();
			if (this.fileName.startsWith(path)) {
				returnSet.add(this.fileName);
			}
			return returnSet;
		}

		@Override
		public InputStreamList openStreams(final String relativeTo, final String streamPath) throws IOException {
			final InputStreamList list = new InputStreamList();
			if (!this.fileName.equals(streamPath)) {
				return list;
			}
			final InputStream stream = new ByteArrayInputStream(this.buffer);
			list.add(URI.create(streamPath), stream);
			return list;
		}

	}

	private static final class TestLiquibaseStatementsWriter extends LiquibaseStatementsWriter {

		private final ByteArrayOutputStream buffer;

		private final ByteArrayOutputStream startStream;

		private final Connection connection;

		private long counter = System.currentTimeMillis();

		private TestLiquibaseStatementsWriter(final OutputStream outputStream, final String version,
				final ByteArrayOutputStream buffer, final ByteArrayOutputStream startStream,
				final Connection connection) throws XMLStreamException {
			super(outputStream, version);
			this.buffer = buffer;
			this.startStream = startStream;
			this.connection = connection;
			startNextChangeSet("changeset-" + this.counter++, "Fastnate", "Fastnate Test");
		}

		@Override
		@SuppressWarnings("resource")
		public void flush() throws IOException {
			if (isChangeSetStarted()) {
				try {
					startNextChangeSet("changeset-" + this.counter++, "Fastnate", null);
				} catch (final XMLStreamException e) {
					throw new IOException(e);
				}
				super.flush();
				this.buffer.write("</databaseChangeLog>".getBytes(StandardCharsets.UTF_8));
				try {
					final String fileName = "changelog.xml";
					new Liquibase(fileName, new ByteArrayResourceAccessor(fileName, this.buffer.toByteArray()),
							new JdbcConnection(this.connection)).update("Fastnate");
				} catch (final LiquibaseException e) {
					throw new IOException(e);
				}
				this.buffer.reset();
				this.startStream.writeTo(this.buffer);
			}
		}
	}

	// Scan for entity classes, as JPA only checks the library with the persistence.xml and wouldn't see our test entities
	private static final List<String> MAPPED_CLASSES = new Reflections((Object[]) new String[] { "org.fastnate" })
			.get(SubTypes.of(TypesAnnotated.with(Entity.class, MappedSuperclass.class, Converter.class))
					.asClass(Thread.currentThread().getContextClassLoader()))
			.stream().map(Class::getName).collect(Collectors.toList());

	/** The settings key that indicates which {@link StatementsWriter} to use for tests. */
	public static final String WRITER_KEY = "fastnate.test.writer";

	private EntityManagerFactory emf;

	@Getter(AccessLevel.PROTECTED)
	private EntityManager em;

	@Getter(AccessLevel.PROTECTED)
	private EntitySqlGenerator generator;

	private JpaProviderTestSetup setup;

	/**
	 * Creates a typed JPQL query.
	 *
	 * @param query
	 *            the JPQL query
	 * @param entityClass
	 *            the type of the result object
	 * @param parameters
	 *            the list of parameter names and values
	 * @return the typed query
	 * @throws IOException
	 *             if the generator throws one during flush
	 */
	public <E> TypedQuery<E> createQuery(final String query, final Class<E> entityClass, final String... parameters)
			throws IOException {
		getGenerator().flush();
		final TypedQuery<E> typedQuery = this.em.createQuery(query, entityClass);
		for (int i = 0; i < parameters.length; i += 2) {
			typedQuery.setParameter(parameters[i], parameters[i + 1]);
		}
		return typedQuery;
	}

	/**
	 * Execute some SQL using the {@link #getConnection() current JDBC connection}.
	 *
	 * @param work
	 *            The work to be performed.
	 * @throws SQLException
	 *             if there is a problem with the work
	 */
	protected void executeSql(final SqlRunnable work) throws SQLException {
		this.setup.executeSql(this.em, work);
	}

	/**
	 * Finds all entities of the given entity class.
	 *
	 * @param entityClass
	 *            the class of the entity
	 * @return all entities of that class
	 * @throws IOException
	 *             if the generator throws one during flush
	 */
	public <E> List<E> findResults(final Class<E> entityClass) throws IOException {
		getGenerator().flush();
		final CriteriaQuery<E> query = this.em.getCriteriaBuilder().createQuery(entityClass);
		query.select(query.from(entityClass));
		return this.em.createQuery(query).getResultList();
	}

	/**
	 * Finds all entities for the given query and entity class.
	 *
	 * @param query
	 *            the JPA-QL query
	 * @param entityClass
	 *            the class of the entity
	 * @param parameters
	 *            the list of parameter names and values
	 * @return the result entities for the query
	 * @throws IOException
	 *             if the generator throws one during flush
	 */
	public <E> List<E> findResults(final String query, final Class<E> entityClass, final String... parameters)
			throws IOException {
		return createQuery(query, entityClass, parameters).getResultList();
	}

	/**
	 * Finds the only entity for the given entity class.
	 *
	 * @param entityClass
	 *            the class of the entity
	 * @return the single entity of that class
	 * @throws IOException
	 *             if the generator throws one during flush
	 */
	public <E> E findSingleResult(final Class<E> entityClass) throws IOException {
		getGenerator().flush();
		final CriteriaQuery<E> query = this.em.getCriteriaBuilder().createQuery(entityClass);
		query.select(query.from(entityClass));
		return this.em.createQuery(query).getSingleResult();
	}

	/**
	 * Finds the entity for the given query and entity class.
	 *
	 * @param query
	 *            the JPA-QL query
	 * @param entityClass
	 *            the class of the entity
	 * @param parameters
	 *            the list of parameter names and values
	 * @return the single result entity for the query
	 * @throws IOException
	 *             if the generator throws one during flush
	 */
	public <E> E findSingleResult(final String query, final Class<E> entityClass, final String... parameters)
			throws IOException {
		return createQuery(query, entityClass, parameters).getSingleResult();
	}

	/**
	 * Resolves the connection from the session.
	 *
	 * @return the connection
	 * @throws SQLException
	 *             Indicates a problem getting the connection
	 */
	protected Connection getConnection() throws SQLException {
		return this.setup.getConnection(this.em);
	}

	/**
	 * Properties for configuring the {@link #getGenerator() generator}.
	 *
	 * @return the test specific properties for the generator
	 */
	protected Properties getGeneratorProperties() {
		final Properties properties = new Properties();
		properties.putAll(System.getProperties());
		return properties;
	}

	/**
	 * Build a entity manager factory (with an empty connected database) for testing.
	 */
	@BeforeEach
	public void setup() {
		setup("drop-and-create");
	}

	/**
	 * Build a entity manager factory (with a connected database) for testing.
	 *
	 * @param schemaCreation
	 *            indicates how to initialize the database ("create" vs. "")
	 */
	protected void setup(final String schemaCreation) {
		final Properties properties = getGeneratorProperties();
		properties.setProperty("jakarta.persistence.schema-generation.database.action", schemaCreation);
		properties.setProperty("jakarta.persistence.create-database-schemas", "true");
		if (properties.getProperty("jakarta.persistence.jdbc.url") == null) {
			properties.setProperty("jakarta.persistence.jdbc.driver", "org.h2.Driver");
			properties.setProperty("jakarta.persistence.jdbc.url",
					"jdbc:h2:mem:fastnate;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=LEGACY");
			properties.setProperty("jakarta.persistence.jdbc.user", "sa");
			properties.setProperty("jakarta.persistence.jdbc.password", "sa");
		}

		this.setup = ServiceLoader.load(JpaProviderTestSetup.class).findFirst().orElseThrow();
		this.setup.initialize(properties);

		final GeneratorContext context = new GeneratorContext(properties);
		context.setMaxUniqueProperties(0);
		this.setup.initialize(context);

		final PersistenceProvider persistenceProvider = ServiceLoader.load(PersistenceProvider.class).findFirst()
				.orElseThrow();

		final String persistenceUnit = properties.getProperty(GeneratorContext.PERSISTENCE_UNIT_KEY, "test-h2");
		final TestPersistenceUnitInfo pu = new TestPersistenceUnitInfo(persistenceUnit,
				persistenceProvider.getClass().getName(), MAPPED_CLASSES, properties);

		this.emf = persistenceProvider.createContainerEntityManagerFactory(pu, properties);
		this.em = this.emf.createEntityManager();

		final String writerKey = context.getSettings().getProperty(WRITER_KEY);
		if (ConnectedStatementsWriter.class.getSimpleName().equals(writerKey)) {
			try {
				context.getSettings().setProperty(ConnectedStatementsWriter.MAX_BATCH_SIZE_KEY, "0");
				this.generator = new EntitySqlGenerator(context, getConnection());
				getConnection().setAutoCommit(true);
			} catch (final SQLException e) {
				throw new IllegalStateException(e);
			}
		} else if (LiquibaseStatementsWriter.class.getSimpleName().equals(writerKey)) {
			try {
				final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				final ByteArrayOutputStream startStream = new ByteArrayOutputStream();
				this.generator = new EntitySqlGenerator(context,
						new TestLiquibaseStatementsWriter(buffer, "4.12", buffer, startStream, getConnection()));
				this.generator.getWriter().flush();
				buffer.writeTo(startStream);
				startStream.write('>');
			} catch (final XMLStreamException | IOException | SQLException e) {
				throw new IllegalStateException(e);
			}
		} else {
			final SqlEmWriter sqlWriter = new SqlEmWriter(this.em);
			this.generator = new EntitySqlGenerator(context, sqlWriter);
		}
	}

	/**
	 * Close the entity manager factory.
	 */
	@AfterEach
	public void tearDown() {
		if (this.generator != null && this.generator.getWriter() instanceof ConnectedStatementsWriter) {
			try {
				((ConnectedStatementsWriter) this.generator.getWriter()).getConnection().close();
			} catch (final SQLException e) {
				// Ignore
			}
		}
		if (this.em != null) {
			this.em.close();
			this.em = null;
		}
		if (this.emf != null) {
			this.emf.close();
			this.emf = null;
		}
	}

	/**
	 * Writes the entity with the generator.
	 *
	 * @param entity
	 *            the entity to write
	 * @throws IOException
	 *             if the generator throws one
	 */
	protected <E> void write(final E entity) throws IOException {
		this.generator.write(entity);
	}
}
