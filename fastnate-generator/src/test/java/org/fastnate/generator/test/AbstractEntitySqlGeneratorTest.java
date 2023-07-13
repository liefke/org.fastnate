package org.fastnate.generator.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import javax.xml.stream.XMLStreamException;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.LiquibaseStatementsWriter;
import org.fastnate.generator.statements.StatementsWriter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.SessionImpl;
import org.junit.After;
import org.junit.Before;

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
	private final class ByteArrayResourceAccessor extends AbstractResourceAccessor {

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
			if (!this.fileName.equals(streamPath)) {
				return null;
			}
			final InputStream stream = new ByteArrayInputStream(this.buffer);
			final InputStreamList list = new InputStreamList();
			list.add(URI.create(streamPath), stream);
			return list;
		}
	}

	/** The settings key that indicates which {@link StatementsWriter} to use for tests. */
	public static final String WRITER_KEY = "fastnate.test.writer";

	private EntityManagerFactory emf;

	@Getter(AccessLevel.PROTECTED)
	private EntityManager em;

	@Getter(AccessLevel.PROTECTED)
	private EntitySqlGenerator generator;

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
		return this.em.unwrap(SessionImpl.class).getJdbcConnectionAccess().obtainConnection();
	}

	/**
	 * Properties for configuring the {@link #getGenerator() generator}.
	 *
	 * @return the test specific properties for the generator
	 */
	protected Properties getGeneratorProperties() {
		final Properties properties = new Properties(System.getProperties());
		properties.putIfAbsent("hibernate.show_sql", "true");
		properties.putIfAbsent("hibernate.format_sql", "false");
		properties.putIfAbsent("hibernate.use_sql_comments", "true");
		return properties;
	}

	/**
	 * Build a entity manager factory (with an empty connected database) for testing.
	 */
	@Before
	public void setup() {
		setup("create-drop");
	}

	/**
	 * Build a entity manager factory (with a connected database) for testing.
	 *
	 * @param schemaCreation
	 *            indicates how to initialize the database ("create" vs. "")
	 */
	protected void setup(final String schemaCreation) {
		final Properties properties = new Properties(System.getProperties());
		properties.setProperty(AvailableSettings.HBM2DDL_AUTO, schemaCreation);

		final GeneratorContext context = new GeneratorContext(getGeneratorProperties());
		context.setMaxUniqueProperties(0);
		if (!context.getDialect().isIdentitySupported()) {
			// Adjust entity manager to allow Identity in model
			properties.setProperty(AvailableSettings.DIALECT_RESOLVERS,
					AllowMissingIdentitySupportDialectResolver.class.getName());
		}

		this.emf = Persistence.createEntityManagerFactory(
				properties.getProperty(GeneratorContext.PERSISTENCE_UNIT_KEY, "test-h2"), properties);
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
				this.generator = new EntitySqlGenerator(context, new LiquibaseStatementsWriter(buffer, "4.12") {

					private long counter = System.currentTimeMillis();

					{
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
							buffer.write("</databaseChangeLog>".getBytes(StandardCharsets.UTF_8));
							try {
								final String fileName = "changelog.xml";
								new Liquibase(fileName, new ByteArrayResourceAccessor(fileName, buffer.toByteArray()),
										new JdbcConnection(getConnection())).update("Fastnate");
							} catch (LiquibaseException | SQLException e) {
								throw new IOException(e);
							}
							buffer.reset();
							startStream.writeTo(buffer);
						}
					}
				});
				this.generator.getWriter().flush();
				buffer.writeTo(startStream);
				startStream.write('>');
			} catch (final XMLStreamException | IOException e) {
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
	@After
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
