package org.fastnate.generator.test.performance;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.dialect.PostgresDialect;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.ListStatementsWriter;
import org.fastnate.generator.statements.PostgreSqlBulkWriter;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.hibernate.Session;
import org.junit.Test;

import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

/**
 * Tests the performance against the currently configured test database.
 *
 * @author Tobias Liefke
 */
@Slf4j
public class PerformanceTest extends AbstractEntitySqlGeneratorTest {

	private PerformanceTestEntity createRootEntity(final long seed, final int maxElementsPerEntity) {
		final Random random = new Random(seed);
		final PerformanceTestEntity root = new PerformanceTestEntity(
				Long.toString(random.nextLong(), Character.MAX_RADIX));
		fillEntity(root, maxElementsPerEntity, random);
		return root;
	}

	private void fillEntity(final PerformanceTestEntity entity, final int maxElementsPerEntity, final Random random) {
		random.longs(random.nextInt(maxElementsPerEntity)).mapToObj(l -> Long.toString(l, Character.MAX_RADIX))
				.forEachOrdered(s -> entity.getStrings().add(s));
		if (maxElementsPerEntity > 1) {
			random.longs(random.nextInt(maxElementsPerEntity)).sequential()
					.mapToObj(l -> new PerformanceTestEntity(entity, Long.toString(l, Character.MAX_RADIX)))
					.forEachOrdered(child -> fillEntity(child, maxElementsPerEntity - 1, random));
		}
	}

	@Override
	protected Properties getGeneratorProperties() {
		final Properties properties = super.getGeneratorProperties();
		properties.put("hibernate.show_sql", "false");
		return properties;
	}

	/**
	 * Tests the performance of fastnate with the {@link PostgreSqlBulkWriter}.
	 */
	@Test
	public void testFastnateBulk() {
		if (getGenerator().getContext().getDialect() instanceof PostgresDialect) {
			((Session) getEm().getDelegate()).doWork(connection -> {
				try {
					final StringWriter sql = new StringWriter();

					final File directory = new File("target", "test-sql");
					directory.mkdirs();
					@SuppressWarnings("resource")
					final PostgreSqlBulkWriter writer = new PostgreSqlBulkWriter(directory, sql,
							StandardCharsets.UTF_8);
					try (EntitySqlGenerator generator = new EntitySqlGenerator(getGenerator().getContext(), writer);
							Statement statement = connection.createStatement()) {
						writer.truncateTables(getGenerator().getContext());
						this.<List<String>> testHugeAmount(entity -> {
							try {
								generator.write(entity);
								// Add a plain statement to ensure that all files are closed
								generator.getWriter().writePlainStatement(getGenerator().getContext().getDialect(),
										"UPDATE PerformanceTestEntity SET id = 1 WHERE id = 1");
								generator.flush();
								final List<String> result = Arrays.asList(sql.toString().split(";\n"));
								sql.getBuffer().setLength(0);
								return result;
							} catch (final IOException e) {
								throw new RuntimeException(e);
							}
						}, statements -> {
							try {
								for (final String stmt : statements) {
									statement.executeUpdate(stmt);
								}
							} catch (final SQLException e) {
								throw new IllegalArgumentException(e);
							}
						});
					}
					for (final File file : writer.getGeneratedFiles()) {
						file.delete();
					}
				} catch (final IOException e) {
					throw new IllegalStateException(e);
				}
			});
		}
	}

	/**
	 * Tests the performance of fastnate with the {@link ConnectedStatementsWriter}.
	 */
	@Test
	public void testFastnateConnected() {
		((Session) getEm().getDelegate()).doWork(connection -> {
			try {
				try (EntitySqlGenerator generator = new EntitySqlGenerator(getGenerator().getContext(), connection)) {
					testHugeAmount(Function.<PerformanceTestEntity> identity(), entity -> {
						try {
							generator.write(entity);
							generator.flush();
						} catch (final IOException e) {
							throw new IllegalStateException(e);
						}
					});
				}
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	/**
	 * Tests the performance of fastnate with predefined SQL.
	 */
	@Test
	public void testFastnatePrebuild() {
		((Session) getEm().getDelegate()).doWork(connection -> {
			final boolean fastInTransaction = getGenerator().getContext().getDialect().isFastInTransaction();
			connection.setAutoCommit(!fastInTransaction);
			try (ListStatementsWriter writer = new ListStatementsWriter();
					EntitySqlGenerator generator = new EntitySqlGenerator(getGenerator().getContext(), writer);
					Statement statement = connection.createStatement()) {
				testHugeAmount(entity -> {
					try {
						generator.write(entity);
						generator.flush();
						final ArrayList<String> result = new ArrayList<>(writer.getStatements());
						writer.getStatements().clear();
						return result;
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}, statements -> {
					try {
						for (final String stmt : statements) {
							statement.executeUpdate(stmt);
						}
						if (fastInTransaction) {
							connection.commit();
						}
					} catch (final SQLException e) {
						throw new IllegalStateException(e);
					}
				});
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		});

	}

	/**
	 * Tests to write many {@link PerformanceTestEntity} to measure the time it takes with different implementations and
	 * databases.
	 *
	 * @param transformation
	 *            used to transform the entity (e.g. into a list of SQL statements)
	 *
	 * @param writer
	 *            used to write the entity
	 */
	private <T> void testHugeAmount(final Function<? super PerformanceTestEntity, T> transformation,
			final Consumer<T> writer) {
		final int maxElementsPerEntity = Integer
				.parseInt(System.getProperty("fastnate.test.performance.max.size", "5"));

		// Warm up
		writer.accept(transformation.apply(createRootEntity(-1, maxElementsPerEntity)));

		// Run measurement
		final Stopwatch stopwatch = Stopwatch.createUnstarted();
		IntStream.range(0, Integer.parseInt(System.getProperty("fastnate.test.performance.rounds", "5")))
				.forEachOrdered(seed -> {
					final T values = transformation.apply(createRootEntity(seed, maxElementsPerEntity));
					stopwatch.start();
					writer.accept(values);
					stopwatch.stop();
				});
		log.info("Writing took: " + stopwatch);
	}

	/**
	 * Tests the performance of the JPA library.
	 */
	@Test
	public void testJpa() {
		testHugeAmount(Function.identity(), entity -> {
			getEm().getTransaction().begin();
			getEm().persist(entity);
			getEm().getTransaction().commit();
		});
	}

}
