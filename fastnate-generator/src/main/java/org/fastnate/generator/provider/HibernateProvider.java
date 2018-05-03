package org.fastnate.generator.provider;

import java.util.Properties;

import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.dialect.H2Dialect;
import org.fastnate.generator.dialect.MsSqlDialect;
import org.fastnate.generator.dialect.MySqlDialect;
import org.fastnate.generator.dialect.OracleDialect;
import org.fastnate.generator.dialect.PostgresDialect;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.hibernate.cfg.AvailableSettings;

/**
 * Encapsulates implementation details of Hibernate as JPA provider.
 *
 * @author Tobias Liefke
 */
public class HibernateProvider implements JpaProvider {

	private static Class<? extends GeneratorDialect> getGeneratorDialectFromConnectionDriver(
			final Properties settings) {
		final String connectionDriver = settings.getProperty(AvailableSettings.DRIVER);
		if (connectionDriver != null) {
			if (connectionDriver.contains("postgresql")) {
				return PostgresDialect.class;
			}
			if (connectionDriver.contains("mysql")) {
				return MySqlDialect.class;
			}
			if (connectionDriver.contains("sqlserver")) {
				return MsSqlDialect.class;
			}
			if (connectionDriver.contains(".h2.")) {
				return H2Dialect.class;
			}
			if (connectionDriver.contains("oracle")) {
				return OracleDialect.class;
			}
		}
		return null;
	}

	private static Class<? extends GeneratorDialect> getGeneratorDialectFromConnectionUrl(final Properties settings) {
		final String connectionUrl = settings.getProperty(AvailableSettings.URL);
		if (connectionUrl != null) {
			if (connectionUrl.contains(":oracle:")) {
				return OracleDialect.class;
			}
			if (connectionUrl.contains(":postgresql:")) {
				return PostgresDialect.class;
			}
			if (connectionUrl.contains(":mysql:")) {
				return MySqlDialect.class;
			}
			if (connectionUrl.contains(":sqlserver:")) {
				return MsSqlDialect.class;
			}
			if (connectionUrl.contains(":h2:")) {
				return H2Dialect.class;
			}
		}
		return null;
	}

	private static Class<? extends GeneratorDialect> getGeneratorDialectFromHibernateDialect(
			final Properties settings) {
		final String hibernateDialect = settings.getProperty(AvailableSettings.DIALECT);
		if (hibernateDialect != null) {
			if (hibernateDialect.contains("Oracle")) {
				return OracleDialect.class;
			}
			if (hibernateDialect.contains("PostgreSQL")) {
				return PostgresDialect.class;
			}
			if (hibernateDialect.contains("MySQL")) {
				return MySqlDialect.class;
			}
			if (hibernateDialect.contains("SQLServer")) {
				return MsSqlDialect.class;
			}
			if (hibernateDialect.contains("H2Dialect")) {
				return H2Dialect.class;
			}
		}
		return null;
	}

	@Override
	public String getDefaultGeneratorTable() {
		return "hibernate_sequences";
	}

	@Override
	public String getDefaultGeneratorTablePkColumnName() {
		return "sequence_name";
	}

	@Override
	public String getDefaultGeneratorTablePkColumnValue() {
		// Hibernate is usually returning the table name as a default column value
		return "";
	}

	@Override
	public String getDefaultGeneratorTableValueColumnName() {
		return "next_val";
	}

	@Override
	public String getDefaultSequence() {
		return "hibernate_sequence";
	}

	@Override
	public void initialize(final Properties settings) {
		if (!settings.containsKey(GeneratorContext.DIALECT_KEY)) {
			// Try to determine the dialect dynamically
			Class<? extends GeneratorDialect> dialect = getGeneratorDialectFromHibernateDialect(settings);
			if (dialect == null) {
				dialect = getGeneratorDialectFromConnectionUrl(settings);
				if (dialect == null) {
					dialect = getGeneratorDialectFromConnectionDriver(settings);
				}
			}
			if (dialect != null) {
				settings.setProperty(GeneratorContext.DIALECT_KEY, dialect.getName());
			}
		}

		if (!settings.containsKey(ConnectedStatementsWriter.LOG_STATEMENTS_KEY)
				&& settings.containsKey(AvailableSettings.SHOW_SQL)) {
			settings.setProperty(ConnectedStatementsWriter.LOG_STATEMENTS_KEY,
					settings.getProperty(AvailableSettings.SHOW_SQL));
		}
	}

	@Override
	public boolean isJoinedDiscriminatorNeeded() {
		return false;
	}

}
