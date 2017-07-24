package org.fastnate.generator.test.performance;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.ListStatementsWriter;
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
						final ArrayList<String> result = new ArrayList<>(writer.getStatements());
						writer.getStatements().clear();
						return result;
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}, statements -> {
					try {
						if (connection.getMetaData().supportsBatchUpdates()) {
							for (final String stmt : statements) {
								statement.addBatch(stmt);
							}
							statement.executeBatch();
						} else {
							for (final String stmt : statements) {
								statement.executeUpdate(stmt);
							}
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
		final int maxElementsPerEntity = Integer.parseInt(System.getProperty("maxElementsPerEntity", "5"));

		// Warm up
		writer.accept(transformation.apply(createRootEntity(-1, maxElementsPerEntity)));

		// Run measurement
		final Stopwatch stopwatch = Stopwatch.createUnstarted();
		IntStream.range(0, Integer.parseInt(System.getProperty("rounds", "5"))).forEachOrdered(seed -> {
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
