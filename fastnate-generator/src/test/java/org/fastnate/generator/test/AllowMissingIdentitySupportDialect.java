package org.fastnate.generator.test;

import jakarta.persistence.GenerationType;
import lombok.RequiredArgsConstructor;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.ColumnAliasExtractor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.LobMergeStrategy;
import org.hibernate.dialect.identity.GetGeneratedKeysDelegate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Hibernate Dialgect which allows {@link GenerationType#IDENTITY} in model, even if the database does not support
 * this.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class AllowMissingIdentitySupportDialect extends Dialect {

	private final Dialect wrapped;



	@Override
	public String appendLockHint(final LockOptions lockOptions, final String tableName) {
		return this.wrapped.appendLockHint(lockOptions, tableName);
	}

	@Override
	public String applyLocksToSql(final String sql, final LockOptions aliasedLockOptions,
			final Map<String, String[]> keyColumnNames) {
		return this.wrapped.applyLocksToSql(sql, aliasedLockOptions, keyColumnNames);
	}


	@Override
	public IdentifierHelper buildIdentifierHelper(final IdentifierHelperBuilder builder,
			final DatabaseMetaData dbMetaData) throws SQLException {
		return this.wrapped.buildIdentifierHelper(builder, dbMetaData);
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return this.wrapped.buildSQLExceptionConversionDelegate();
	}


	@Override
	public boolean canCreateCatalog() {
		return this.wrapped.canCreateCatalog();
	}

	@Override
	public boolean canCreateSchema() {
		return this.wrapped.canCreateSchema();
	}




	@Override
	public char closeQuote() {
		return this.wrapped.closeQuote();
	}

	@Override
	public void contributeTypes(final TypeContributions typeContributions, final ServiceRegistry serviceRegistry) {
		this.wrapped.contributeTypes(typeContributions, serviceRegistry);
	}




	@Override
	public ScrollMode defaultScrollMode() {
		return this.wrapped.defaultScrollMode();
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return this.wrapped.doesReadCommittedCauseWritersToBlockReaders();
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return this.wrapped.doesRepeatableReadCauseReadersToBlockWriters();
	}

	@Override
	public boolean dropConstraints() {
		return this.wrapped.dropConstraints();
	}

	@Override
	public boolean equals(final Object obj) {
		return this.wrapped.equals(obj);
	}


	@Override
	public boolean forceLobAsLastValue() {
		return this.wrapped.forceLobAsLastValue();
	}


	@Override
	public String getAddColumnString() {
		return this.wrapped.getAddColumnString();
	}

	@Override
	public String getAddColumnSuffixString() {
		return this.wrapped.getAddColumnSuffixString();
	}

	@Override
	public String getAddForeignKeyConstraintString(final String constraintName, final String[] foreignKey,
			final String referencedTable, final String[] primaryKey, final boolean referencesPrimaryKey) {
		return this.wrapped.getAddForeignKeyConstraintString(constraintName, foreignKey, referencedTable, primaryKey,
				referencesPrimaryKey);
	}

	@Override
	public String getAddPrimaryKeyConstraintString(final String constraintName) {
		return this.wrapped.getAddPrimaryKeyConstraintString(constraintName);
	}


	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return this.wrapped.getCallableStatementSupport();
	}

	@Override
	public String getCascadeConstraintsString() {
		return this.wrapped.getCascadeConstraintsString();
	}

	@Override
	public String getCaseInsensitiveLike() {
		return this.wrapped.getCaseInsensitiveLike();
	}


	@Override
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return this.wrapped.getColumnAliasExtractor();
	}

	@Override
	public String getColumnComment(final String comment) {
		return this.wrapped.getColumnComment(comment);
	}

	@Override
	public String[] getCreateCatalogCommand(final String catalogName) {
		return this.wrapped.getCreateCatalogCommand(catalogName);
	}

	@Override
	public String getCreateMultisetTableString() {
		return this.wrapped.getCreateMultisetTableString();
	}

	@Override
	public String[] getCreateSchemaCommand(final String schemaName) {
		return this.wrapped.getCreateSchemaCommand(schemaName);
	}



	@Override
	public String getCreateTableString() {
		return this.wrapped.getCreateTableString();
	}


	@Override
	public String getCurrentSchemaCommand() {
		return this.wrapped.getCurrentSchemaCommand();
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return this.wrapped.getCurrentTimestampSelectString();
	}



	@Override
	public String[] getDropCatalogCommand(final String catalogName) {
		return this.wrapped.getDropCatalogCommand(catalogName);
	}

	@Override
	public String getDropForeignKeyString() {
		return this.wrapped.getDropForeignKeyString();
	}

	@Override
	public String[] getDropSchemaCommand(final String schemaName) {
		return this.wrapped.getDropSchemaCommand(schemaName);
	}


	@Override
	public String getDropTableString(final String tableName) {
		return this.wrapped.getDropTableString(tableName);
	}

	@Override
	public String getForUpdateNowaitString() {
		return this.wrapped.getForUpdateNowaitString();
	}

	@Override
	public String getForUpdateNowaitString(final String aliases) {
		return this.wrapped.getForUpdateNowaitString(aliases);
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return this.wrapped.getForUpdateSkipLockedString();
	}

	@Override
	public String getForUpdateSkipLockedString(final String aliases) {
		return this.wrapped.getForUpdateSkipLockedString(aliases);
	}

	@Override
	public String getForUpdateString() {
		return this.wrapped.getForUpdateString();
	}

	@Override
	public String getForUpdateString(final LockMode lockMode) {
		return this.wrapped.getForUpdateString(lockMode);
	}

	@Override
	public String getForUpdateString(final LockOptions lockOptions) {
		return this.wrapped.getForUpdateString(lockOptions);
	}

	@Override
	public String getForUpdateString(final String aliases) {
		return this.wrapped.getForUpdateString(aliases);
	}

	@Override
	public String getForUpdateString(final String aliases, final LockOptions lockOptions) {
		return this.wrapped.getForUpdateString(aliases, lockOptions);
	}






	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		final IdentityColumnSupport identityColumnSupport = this.wrapped.getIdentityColumnSupport();
		return new IdentityColumnSupport() {

			@Override
			public String appendIdentitySelectToInsert(final String insertString) {
				return identityColumnSupport.appendIdentitySelectToInsert(insertString);
			}

			@Override
			public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(final PostInsertIdentityPersister persister,
					final Dialect dialect) {
				return identityColumnSupport.buildGetGeneratedKeysDelegate(persister, dialect);
			}

			@Override
			public String getIdentityColumnString(final int type) throws MappingException {
				try {
					return identityColumnSupport.getIdentityColumnString(type);
				} catch (final MappingException e) {
					// Ignore
					return "";
				}
			}

			@Override
			public String getIdentityInsertString() {
				return identityColumnSupport.getIdentityInsertString();
			}

			@Override
			public String getIdentitySelectString(final String table, final String column, final int type)
					throws MappingException {
				return identityColumnSupport.getIdentitySelectString(table, column, type);
			}

			@Override
			public boolean hasDataTypeInIdentityColumn() {
				return identityColumnSupport.hasDataTypeInIdentityColumn();
			}

			@Override
			public boolean supportsIdentityColumns() {
				return identityColumnSupport.supportsIdentityColumns();
			}

			@Override
			public boolean supportsInsertSelectIdentity() {
				return identityColumnSupport.supportsInsertSelectIdentity();
			}
		};
	}



	@Override
	public int getInExpressionCountLimit() {
		return this.wrapped.getInExpressionCountLimit();
	}

	@Override
	public Set<String> getKeywords() {
		return this.wrapped.getKeywords();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return this.wrapped.getLimitHandler();
	}



	@Override
	public LobMergeStrategy getLobMergeStrategy() {
		return this.wrapped.getLobMergeStrategy();
	}

	@Override
	public LockingStrategy getLockingStrategy(final Lockable lockable, final LockMode lockMode) {
		return this.wrapped.getLockingStrategy(lockable, lockMode);
	}

	@Override
	public String getLowercaseFunction() {
		return this.wrapped.getLowercaseFunction();
	}

	@Override
	public int getMaxAliasLength() {
		return this.wrapped.getMaxAliasLength();
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return this.wrapped.getNameQualifierSupport();
	}



	@Override
	public String getNoColumnsInsertString() {
		return this.wrapped.getNoColumnsInsertString();
	}



	@Override
	public String getNullColumnString() {
		return this.wrapped.getNullColumnString();
	}

	@Override
	public String getQueryHintString(final String query, final List<String> hints) {
		return this.wrapped.getQueryHintString(query, hints);
	}

	@Override
	public String getQuerySequencesString() {
		return this.wrapped.getQuerySequencesString();
	}

	@Override
	public String getReadLockString(final int timeout) {
		return this.wrapped.getReadLockString(timeout);
	}

	@Override
	public ResultSet getResultSet(final CallableStatement statement) throws SQLException {
		return this.wrapped.getResultSet(statement);
	}

	@Override
	public ResultSet getResultSet(final CallableStatement statement, final int position) throws SQLException {
		return this.wrapped.getResultSet(statement, position);
	}

	@Override
	public ResultSet getResultSet(final CallableStatement statement, final String name) throws SQLException {
		return this.wrapped.getResultSet(statement, name);
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		return this.wrapped.getSchemaNameResolver();
	}


	@Override
	public String getSelectGUIDString() {
		return this.wrapped.getSelectGUIDString();
	}



	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return this.wrapped.getSequenceInformationExtractor();
	}



	@Override
	public String getTableComment(final String comment) {
		return this.wrapped.getTableComment(comment);
	}

	@Override
	public String getTableTypeString() {
		return this.wrapped.getTableTypeString();
	}


	@Override
	public UniqueDelegate getUniqueDelegate() {
		return this.wrapped.getUniqueDelegate();
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return this.wrapped.getViolatedConstraintNameExtractor();
	}

	@Override
	public String getWriteLockString(final int timeout) {
		return this.wrapped.getWriteLockString(timeout);
	}

	@Override
	public boolean hasAlterTable() {
		return this.wrapped.hasAlterTable();
	}


	@Override
	public int hashCode() {
		return this.wrapped.hashCode();
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return this.wrapped.hasSelfReferentialForeignKeyBug();
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return this.wrapped.isCurrentTimestampSelectStringCallable();
	}

	@Override
	public boolean isLockTimeoutParameterized() {
		return this.wrapped.isLockTimeoutParameterized();
	}

	@Override
	public char openQuote() {
		return this.wrapped.openQuote();
	}

	@Override
	public boolean qualifyIndexName() {
		return this.wrapped.qualifyIndexName();
	}

	@Override
	public int registerResultSetOutParameter(final CallableStatement statement, final int position)
			throws SQLException {
		return this.wrapped.registerResultSetOutParameter(statement, position);
	}

	@Override
	public int registerResultSetOutParameter(final CallableStatement statement, final String name) throws SQLException {
		return this.wrapped.registerResultSetOutParameter(statement, name);
	}


	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return this.wrapped.requiresParensForTupleDistinctCounts();
	}

	@Override
	public boolean supportsBindAsCallableArgument() {
		return this.wrapped.supportsBindAsCallableArgument();
	}

	@Override
	public boolean supportsCascadeDelete() {
		return this.wrapped.supportsCascadeDelete();
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return this.wrapped.supportsCaseInsensitiveLike();
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return this.wrapped.supportsCircularCascadeDeleteConstraints();
	}

	@Override
	public boolean supportsColumnCheck() {
		return this.wrapped.supportsColumnCheck();
	}

	@Override
	public boolean supportsCommentOn() {
		return this.wrapped.supportsCommentOn();
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return this.wrapped.supportsCurrentTimestampSelection();
	}


	@Override
	public boolean supportsExistsInSelect() {
		return this.wrapped.supportsExistsInSelect();
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return this.wrapped.supportsExpectedLobUsagePattern();
	}



	@Override
	public boolean supportsIfExistsAfterConstraintName() {
		return this.wrapped.supportsIfExistsAfterConstraintName();
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return this.wrapped.supportsIfExistsAfterTableName();
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return this.wrapped.supportsIfExistsBeforeConstraintName();
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return this.wrapped.supportsIfExistsBeforeTableName();
	}



	@Override
	public boolean supportsLockTimeouts() {
		return this.wrapped.supportsLockTimeouts();
	}



	@Override
	public boolean supportsOuterJoinForUpdate() {
		return this.wrapped.supportsOuterJoinForUpdate();
	}

	@Override
	public boolean supportsParametersInInsertSelect() {
		return this.wrapped.supportsParametersInInsertSelect();
	}



	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return this.wrapped.supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
	}



	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return this.wrapped.supportsSubqueryOnMutatingTable();
	}

	@Override
	public boolean supportsSubselectAsInPredicateLHS() {
		return this.wrapped.supportsSubselectAsInPredicateLHS();
	}

	@Override
	public boolean supportsTableCheck() {
		return this.wrapped.supportsTableCheck();
	}

	@Override
	public boolean supportsTupleCounts() {
		return this.wrapped.supportsTupleCounts();
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return this.wrapped.supportsTupleDistinctCounts();
	}



	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return this.wrapped.supportsUnboundedLobLocatorMaterialization();
	}

	@Override
	public boolean supportsUnionAll() {
		return this.wrapped.supportsUnionAll();
	}



	@Override
	public String toBooleanValueString(final boolean bool) {
		return this.wrapped.toBooleanValueString(bool);
	}

	@Override
	public String toString() {
		return this.wrapped.toString();
	}

	@Override
	public String transformSelectString(final String select) {
		return this.wrapped.transformSelectString(select);
	}


	@Override
	public boolean useInputStreamToInsertBlob() {
		return this.wrapped.useInputStreamToInsertBlob();
	}



}
