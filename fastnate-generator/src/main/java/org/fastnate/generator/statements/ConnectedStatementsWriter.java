package org.fastnate.generator.statements;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fastnate.generator.context.DefaultContextModelListener;
import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.context.IdGenerator;
import org.fastnate.generator.context.SequenceIdGenerator;
import org.fastnate.generator.dialect.GeneratorDialect;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * An implementation of a {@link StatementsWriter} which writes the SQL statements directly to a database connection.
 *
 * @author Tobias Liefke
 */
@Slf4j
public class ConnectedStatementsWriter extends AbstractStatementsWriter {

	private static final class PreparedInsertStatement extends InsertStatement {

		@Getter
		private final PreparedStatement statement;

		private final int columnCount;

		@Getter
		private final String sql;

		private final BitSet availableColumns;

		private final int[] parameterIndices;

		PreparedInsertStatement(final GeneratorDialect dialect, final Connection connection, final GeneratorTable table)
				throws SQLException {
			super(dialect, table);

			final StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(getTable().getName()).append(' ');
			final Collection<GeneratorColumn> columns = table.getColumns().values();
			this.columnCount = columns.size();
			this.parameterIndices = new int[this.columnCount];
			int parameterCount = 0;
			for (final GeneratorColumn column : columns) {
				if (!column.isAutoGenerated()) {
					sqlBuilder.append(parameterCount > 0 ? ',' : '(');
					sqlBuilder.append(column.getName());
					this.parameterIndices[column.getIndex()] = ++parameterCount;
				}
			}

			if (parameterCount == 0) {
				sqlBuilder.append(dialect.getEmptyValuesExpression());
			} else {
				sqlBuilder.append(") VALUES (");
				sqlBuilder.append('?');
				while (--parameterCount > 0) {
					sqlBuilder.append(",?");
				}
				sqlBuilder.append(')');
			}
			this.sql = sqlBuilder.toString();
			this.statement = connection.prepareStatement(this.sql);
			this.availableColumns = new BitSet(this.columnCount);
		}

		public void close() throws SQLException {
			this.statement.close();
		}

		public int executeUpdate() throws SQLException {
			if (this.availableColumns.cardinality() != this.columnCount) {
				for (int i = 0; i < this.columnCount; i++) {
					if (!this.availableColumns.get(i)) {
						final int parameterIndex = this.parameterIndices[i];
						if (parameterIndex > 0) {
							this.statement.setObject(parameterIndex, null);
						}
					}
				}
			}

			return this.statement.executeUpdate();
		}

		@Override
		public void reset() {
			this.availableColumns.clear();
			super.reset();
		}

		@Override
		public void setColumnValue(final GeneratorColumn column, final ColumnExpression expression) {
			super.setColumnValue(column, expression);
			if (!isPlainExpressionAvailable()) {
				try {
					final int index = column.getIndex();
					this.availableColumns.set(index);
					final int parameterIndex = this.parameterIndices[index];
					if (parameterIndex <= 0) {
						throw new IllegalArgumentException("Can't set auto generated column " + column.getName());
					}
					this.statement.setObject(parameterIndex, ((PrimitiveColumnExpression<?>) expression).getValue());
				} catch (final SQLException e) {
					throw new IllegalArgumentException("Can't set " + column + " to " + expression + " in " + this.sql,
							e);
				}
			}
		}

	}

	/**
	 * Name of the setting which controls the maximum size of generated batches. If set to something below 2, no batches
	 * are used.
	 */
	public static final String MAX_BATCH_SIZE_KEY = "fastnate.generator.max.batch";

	/** Name of the setting which turns logging of statements on or off. */
	public static final String LOG_STATEMENTS_KEY = "fastnate.generator.log.statements";

	/** The count of milliseconds to wait, until a log message with the current count of statements is written. */
	private static final long MILLISECONDS_BETWEEN_LOG_MESSAGES = 60 * 1000;

	/** The database connection that was used when creating this generator. */
	@Getter
	private final Connection connection;

	/** Indicates that autoCommit was enabled before. */
	private final boolean autoCommitOldState;

	/** Indicates that we have opened a transaction. */
	private final boolean inTransaction;

	/** Indicates that the database connection supports batch statements. */
	private final boolean batchSupported;

	/** Indicates that we log each statement for debugging purposes. */
	private final boolean logStatements;

	/** The maximum count of statements per batch job. */
	private final int maxBatchSize;

	/** Used to execute all plain SQL statements. */
	private final Statement plainStatement;

	/** All prepared statements for the tables. */
	private final List<PreparedInsertStatement> preparedStatements = new ArrayList<>();

	/** All available prepared statements per table. */
	private final Map<GeneratorTable, List<PreparedInsertStatement>> availablePreparedStatements = new HashMap<>();

	/** The count of statements executed in the current batch. */
	private int batchCount;

	/** The last time that we have written a log message about the count of statements. */
	private long lastLogTime;

	/** The count of statements that we have executed up to now. */
	@Getter
	private long statementsCount;

	/**
	 * Creates a new StatementWriter that writes to a database connection.
	 *
	 * @param connection
	 *            the database connection
	 * @param context
	 *            contains the indices for initialization
	 * @throws SQLException
	 *             if the connection is invalid
	 */
	public ConnectedStatementsWriter(final Connection connection, final GeneratorContext context) throws SQLException {
		this.connection = connection;
		this.autoCommitOldState = connection.getAutoCommit();
		this.inTransaction = context.getDialect().isFastInTransaction();
		connection.setAutoCommit(!this.inTransaction);
		this.batchSupported = connection.getMetaData().supportsBatchUpdates();
		this.logStatements = Boolean.parseBoolean(context.getSettings().getProperty(LOG_STATEMENTS_KEY, "false"));
		this.maxBatchSize = Integer.parseInt(context.getSettings().getProperty(MAX_BATCH_SIZE_KEY, "100"));
		final Statement sharedStatement = connection.createStatement();
		this.plainStatement = sharedStatement;

		context.addContextModelListener(new DefaultContextModelListener() {

			@Override
			public void foundGenerator(final IdGenerator generator) {
				// Initialize generator, if necessary
				if (!context.isWriteRelativeIds()) {
					String sql = generator.getExpression(null, null, generator.getCurrentValue(), false);
					if (sql.matches("(SELECT\\W.*)")) {
						sql = sql.substring(1, sql.length() - 1);
					} else {
						sql = "SELECT (" + sql + ") currentValue " + context.getDialect().getOptionalTable();
					}
					try (ResultSet resultSet = sharedStatement.executeQuery(sql)) {
						if (resultSet.next()) {
							final long currentValue = resultSet.getLong(1);
							if (resultSet.wasNull()) {
								return;
							}
							if (generator instanceof SequenceIdGenerator) {
								final SequenceIdGenerator sequence = (SequenceIdGenerator) generator;
								if (sequence.getInitialValue() - sequence.getAllocationSize() == currentValue) {
									return;
								}
							}
							generator.setCurrentValue(currentValue);
						}
					} catch (final SQLException e) {
						if (!(generator instanceof SequenceIdGenerator)) {
							throw new IllegalStateException("Can't initialize generator with " + sql, e);
						}
						// Ignore if sequence.currval is not available for a new sequence - for example in Oracle
					}
				}
			}
		});
	}

	private void checkUpdate(final int updatedRows, final String sql) {
		if (updatedRows != 1) {
			throw new IllegalStateException(
					(updatedRows == 0 ? "No row created for " : "More than one rows created for ") + sql);
		}
		this.statementsCount++;
	}

	@Override
	public void close() throws IOException {
		try {
			closeBatch();
			commit();

			log.info("{} SQL statements successfully executed", this.statementsCount);
			try {
				this.plainStatement.close();
				for (final PreparedInsertStatement stmt : this.preparedStatements) {
					stmt.close();
				}
			} catch (final SQLException e) {
				// Ignore - as this will only happen, if there was already an exception during update
			}
		} finally {
			try {
				this.connection.setAutoCommit(this.autoCommitOldState);
			} catch (final SQLException e) {
				// Ignore
			}
		}
	}

	private void closeBatch() throws IOException {
		if (this.batchCount > 0) {
			try {
				this.plainStatement.executeBatch();
				this.statementsCount += this.batchCount;
			} catch (final SQLException e) {
				throw new IOException("Could not execute statements: " + e, e);
			} finally {
				this.batchCount = 0;
			}
		}
	}

	private void commit() throws IOException {
		if (this.inTransaction) {
			try {
				this.connection.commit();
			} catch (final SQLException e) {
				throw new IOException("Could not commit transaction: " + e, e);
			}
		}
	}

	@Override
	public TableStatement createInsertStatement(final GeneratorDialect dialect, final GeneratorTable table) {
		List<PreparedInsertStatement> availableStatements = this.availablePreparedStatements.get(table);
		if (availableStatements == null) {
			this.availablePreparedStatements.put(table, availableStatements = new ArrayList<>());
		}
		final PreparedInsertStatement insertStatement;
		if (availableStatements.isEmpty()) {
			try {
				this.preparedStatements
						.add(insertStatement = new PreparedInsertStatement(dialect, this.connection, table));
			} catch (final SQLException e) {
				throw new IllegalStateException("Can't generate prepared statement for " + table.getName(), e);
			}
		} else {
			insertStatement = availableStatements.remove(availableStatements.size() - 1);
			insertStatement.reset();
		}
		return insertStatement;
	}

	@Override
	public void flush() throws IOException {
		closeBatch();
		commit();
	}

	@Override
	public void writePlainStatement(final GeneratorDialect dialect, final String sql) throws IOException {
		writePlainStatement(sql);
	}

	private void writePlainStatement(final String sql) throws IOException {
		try {
			closeBatch();
			if (this.logStatements) {
				log.info(sql);
			}
			this.plainStatement.executeUpdate(sql);
			this.statementsCount++;
		} catch (final SQLException e) {
			throw new IOException("Could not execute statement: " + sql, e);
		}
	}

	@Override
	public void writeStatement(final EntityStatement stmt) throws IOException {
		final long currentTime = System.currentTimeMillis();
		if (currentTime - this.lastLogTime >= MILLISECONDS_BETWEEN_LOG_MESSAGES) {
			this.lastLogTime = currentTime;
			if (this.statementsCount > 1) {
				log.info("{} SQL statements executed", this.statementsCount);
			}
		}

		if (stmt instanceof PreparedInsertStatement) {
			final PreparedInsertStatement insert = (PreparedInsertStatement) stmt;
			this.availablePreparedStatements.get(insert.getTable()).add(insert);
			if (!insert.isPlainExpressionAvailable()) {
				closeBatch();
				final String sql = insert.getSql();
				if (this.logStatements) {
					log.info(insert.toSql());
				}
				try {
					checkUpdate(insert.executeUpdate(), sql);
				} catch (final SQLException e) {
					throw new IOException("Could not execute statement: " + sql, e);
				}
			} else {
				writeTableStatement(insert.toSql());
			}
		} else if (stmt instanceof TableStatement) {
			writeTableStatement(stmt.toSql());
		} else {
			writePlainStatement(stmt.toSql());
		}
	}

	private void writeTableStatement(final String sql) throws IOException {
		if (this.logStatements) {
			log.info(sql);
		}
		try {
			if (this.batchSupported && this.maxBatchSize > 1) {
				this.plainStatement.addBatch(sql);
				if (++this.batchCount > this.maxBatchSize) {
					closeBatch();
				}
			} else {
				checkUpdate(this.plainStatement.executeUpdate(sql), sql);
			}
		} catch (final SQLException e) {
			throw new IOException("Could not execute statement: " + sql, e);
		}
	}

}
