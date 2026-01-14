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

import org.apache.commons.lang3.time.StopWatch;
import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.dialect.PostgresDialect;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.ListStatementsWriter;
import org.fastnate.generator.statements.PostgreSqlBulkWriter;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.fastnate.util.ClassUtil;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Tests the performance against the currently configured test database.
 *
 * @author Tobias Liefke
 */
@Slf4j
public class PerformanceTest extends AbstractEntitySqlGeneratorTest {

	private List<PerformanceTestEntity> createRootEntities(final long seed, final int maxElementsPerEntity,
			final int countOfRootEntities) {
		final Random random = new Random(seed);
		final List<PerformanceTestEntity> result = new ArrayList<>(countOfRootEntities);
		for (int i = 0; i < countOfRootEntities; i++) {
			final PerformanceTestEntity root = new PerformanceTestEntity(
					Long.toString(random.nextLong(), Character.MAX_RADIX));
			fillEntity(root, maxElementsPerEntity, random);
			result.add(root);
		}
		return result;
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
		properties.setProperty("hibernate.show_sql", "false");
		return properties;
	}

	/**
	 * Tests the performance of fastnate with the {@link PostgreSqlBulkWriter}.
	 *
	 * @throws SQLException
	 *             if there is a problem with the SQL
	 */
	@Test
	public void testFastnateBulk() throws SQLException {
		if (getGenerator().getContext().getDialect() instanceof PostgresDialect) {
			executeSql(connection -> {
				try {
					final StringWriter sql = new StringWriter();

					final File directory = new File("target", "test-sql");
					directory.mkdirs();
					final PostgreSqlBulkWriter writer = new PostgreSqlBulkWriter(getGenerator().getContext(), directory,
							sql, StandardCharsets.UTF_8);
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
								throw new IllegalStateException(e);
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
	 *
	 * @throws SQLException
	 *             if there is a problem with the SQL
	 */
	@Test
	public void testFastnateConnected() throws SQLException {
		executeSql(connection -> {
			try {
				try (EntitySqlGenerator generator = new EntitySqlGenerator(getGenerator().getContext(), connection)) {
					testHugeAmount(Function.identity(), entity -> {
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
	 *
	 * @throws SQLException
	 *             if there is a problem with the SQL
	 */
	@Test
	public void testFastnatePrebuild() throws SQLException {
		executeSql(connection -> {
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
						throw new IllegalStateException(e);
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
	private <T> void testHugeAmount(final Function<List<? super PerformanceTestEntity>, T> transformation,
			final Consumer<T> writer) {
		final int maxElementsPerEntity = Integer
				.parseInt(System.getProperty("fastnate.test.performance.max.depth", "4"));

		final int rootElements = Integer.parseInt(System.getProperty("fastnate.test.performance.size", "100"));

		// Warm up
		writer.accept(transformation.apply(createRootEntities(-1, maxElementsPerEntity, 2)));

		// Run measurement
		final StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		stopwatch.suspend();
		final int rounds = Integer.parseInt(System.getProperty("fastnate.test.performance.rounds", "5"));
		IntStream.range(0, rounds).forEachOrdered(seed -> {
			final T values = transformation.apply(createRootEntities(seed, maxElementsPerEntity, rootElements));
			stopwatch.resume();
			writer.accept(values);
			stopwatch.suspend();
		});
		log.info("{} - writing took: {}", ClassUtil.getCallerMethod(PerformanceTest.class), stopwatch);
	}

	/**
	 * Tests the performance of the JPA library.
	 */
	@Test
	public void testJpa() {
		testHugeAmount(Function.identity(), entities -> {
			getEm().getTransaction().begin();
			for (final Object entity : entities) {
				getEm().persist(entity);
			}
			getEm().getTransaction().commit();
		});
	}

}
