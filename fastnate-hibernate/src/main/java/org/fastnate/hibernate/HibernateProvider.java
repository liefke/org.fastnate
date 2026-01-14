package org.fastnate.hibernate;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.GenerationType;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.context.Property;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.dialect.H2Dialect;
import org.fastnate.generator.dialect.MsSqlDialect;
import org.fastnate.generator.dialect.MySqlDialect;
import org.fastnate.generator.dialect.OracleDialect;
import org.fastnate.generator.dialect.PostgresDialect;
import org.fastnate.generator.provider.JpaProvider;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * Encapsulates implementation details of Hibernate as JPA provider.
 *
 * @author Tobias Liefke
 */
public class HibernateProvider implements JpaProvider {

	private static Class<? extends GeneratorDialect> getGeneratorDialectFromHibernateDialect(
			final Properties settings) {
		final String hibernateDialect = settings.getProperty(JdbcSettings.DIALECT);
		if (hibernateDialect != null) {
			if (hibernateDialect.contains("Oracle")) {
				return OracleDialect.class;
			}
			if (hibernateDialect.contains("PostgreSQL")) {
				return PostgresDialect.class;
			}
			if (hibernateDialect.contains("MySQL") || hibernateDialect.contains("MariaDB")) {
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
	public <E, X> Property<X, ?> buildProperty(final EntityClass<E> entityClass, final GeneratorTable propertyTable,
			final AttributeAccessor attribute, final Map<String, AttributeOverride> surroundingAttributeOverrides,
			final Map<String, AssociationOverride> surroundingAssociationOverrides) {
		if (!isPersistent(attribute)) {
			return null;
		}

		if (attribute.isAnnotationPresent(ManyToAny.class)) {
			if (Collection.class.isAssignableFrom(attribute.getType())) {
				return new ManyToAnyCollectionProperty<>(entityClass, attribute,
						surroundingAssociationOverrides.get(attribute.getName()),
						surroundingAttributeOverrides.get(attribute.getName()));
			}
			if (Map.class.isAssignableFrom(attribute.getType())) {
				return new ManyToAnyMapProperty<>(entityClass, attribute,
						surroundingAssociationOverrides.get(attribute.getName()),
						surroundingAttributeOverrides.get(attribute.getName()));
			}
			throw new ModelException("Unknown collection type " + attribute.getType() + " for " + attribute);
		}

		if (attribute.isAnnotationPresent(Any.class)) {
			return new AnyEntityProperty<>(entityClass.getContext(), propertyTable, attribute,
					surroundingAssociationOverrides.get(attribute.getName()),
					surroundingAttributeOverrides.get(attribute.getName()));
		}

		return JpaProvider.super.buildProperty(entityClass, propertyTable, attribute, surroundingAttributeOverrides,
				surroundingAssociationOverrides);
	}

	@Override
	public GenerationType getAutoGenerationType(final GeneratorDialect dialect) {
		return dialect.isSequenceSupported() ? GenerationType.SEQUENCE
				: dialect.isIdentitySupported() ? GenerationType.IDENTITY : GenerationType.TABLE;
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
	public String getDefaultGeneratorTableValueColumnName() {
		return "next_val";
	}

	@Override
	public String getDefaultSequence(final String tableName) {
		return tableName + SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX;
	}

	@Override
	public void initialize(final Properties settings) {
		if (!settings.containsKey(GeneratorContext.DIALECT_KEY)) {
			// Try to determine the dialect dynamically
			final Class<? extends GeneratorDialect> dialect = getGeneratorDialectFromHibernateDialect(settings);
			if (dialect != null) {
				settings.setProperty(GeneratorContext.DIALECT_KEY, dialect.getName());
			}
		}

		JpaProvider.copySetting(settings, MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS,
				GeneratorContext.QUOTE_ALL_IDENTIFIERS_KEY);
		JpaProvider.copySetting(settings, JdbcSettings.SHOW_SQL, ConnectedStatementsWriter.LOG_STATEMENTS_KEY);
	}

	@Override
	public boolean isInitializingGeneratorTables() {
		return true;
	}

	@Override
	public boolean isJoinedDiscriminatorNeeded() {
		return false;
	}

	@Override
	public boolean isPersistent(final AttributeAccessor attribute) {
		return JpaProvider.super.isPersistent(attribute) && !attribute.isAnnotationPresent(Formula.class);
	}

}
