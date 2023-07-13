package org.fastnate.generator.test;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.GenerationType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NullPrecedence;
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
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import lombok.RequiredArgsConstructor;

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
	public String appendIdentitySelectToInsert(final String insertString) {
		return this.wrapped.appendIdentitySelectToInsert(insertString);
	}

	@Override
	public String appendLockHint(final LockMode mode, final String tableName) {
		return this.wrapped.appendLockHint(mode, tableName);
	}

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
	public boolean areStringComparisonsCaseInsensitive() {
		return this.wrapped.areStringComparisonsCaseInsensitive();
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return this.wrapped.bindLimitParametersFirst();
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return this.wrapped.bindLimitParametersInReverseOrder();
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
	public SQLExceptionConverter buildSQLExceptionConverter() {
		return this.wrapped.buildSQLExceptionConverter();
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
	public String cast(final String value, final int jdbcTypeCode, final int length) {
		return this.wrapped.cast(value, jdbcTypeCode, length);
	}

	@Override
	public String cast(final String value, final int jdbcTypeCode, final int precision, final int scale) {
		return this.wrapped.cast(value, jdbcTypeCode, precision, scale);
	}

	@Override
	public String cast(final String value, final int jdbcTypeCode, final int length, final int precision,
			final int scale) {
		return this.wrapped.cast(value, jdbcTypeCode, length, precision, scale);
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
	public int convertToFirstRowValue(final int zeroBasedFirstResult) {
		return this.wrapped.convertToFirstRowValue(zeroBasedFirstResult);
	}

	@Override
	public CaseFragment createCaseFragment() {
		return this.wrapped.createCaseFragment();
	}

	@Override
	public JoinFragment createOuterJoinFragment() {
		return this.wrapped.createOuterJoinFragment();
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
	public boolean forceLimitUsage() {
		return this.wrapped.forceLimitUsage();
	}

	@Override
	public boolean forceLobAsLastValue() {
		return this.wrapped.forceLobAsLastValue();
	}

	@Override
	public boolean forUpdateOfColumns() {
		return this.wrapped.forUpdateOfColumns();
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
	public String getAddUniqueConstraintString(final String constraintName) {
		return this.wrapped.getAddUniqueConstraintString(constraintName);
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
	public String getCastTypeName(final int code) {
		return this.wrapped.getCastTypeName(code);
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
	public String[] getCreateSequenceStrings(final String sequenceName) throws MappingException {
		return this.wrapped.getCreateSequenceStrings(sequenceName);
	}

	@Override
	public String[] getCreateSequenceStrings(final String sequenceName, final int initialValue, final int incrementSize)
			throws MappingException {
		return this.wrapped.getCreateSequenceStrings(sequenceName, initialValue, incrementSize);
	}

	@Override
	public String getCreateTableString() {
		return this.wrapped.getCreateTableString();
	}

	@Override
	public String getCrossJoinSeparator() {
		return this.wrapped.getCrossJoinSeparator();
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
	public String getCurrentTimestampSQLFunctionName() {
		return this.wrapped.getCurrentTimestampSQLFunctionName();
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return this.wrapped.getDefaultMultiTableBulkIdStrategy();
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
	public String[] getDropSequenceStrings(final String sequenceName) throws MappingException {
		return this.wrapped.getDropSequenceStrings(sequenceName);
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
	public String getHibernateTypeName(final int code) throws HibernateException {
		return this.wrapped.getHibernateTypeName(code);
	}

	@Override
	public String getHibernateTypeName(final int code, final int length, final int precision, final int scale)
			throws HibernateException {
		return this.wrapped.getHibernateTypeName(code, length, precision, scale);
	}

	@Override
	public String getIdentityColumnString(final int type) throws MappingException {
		return this.wrapped.getIdentityColumnString(type);
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
	public String getIdentityInsertString() {
		return this.wrapped.getIdentityInsertString();
	}

	@Override
	public String getIdentitySelectString(final String table, final String column, final int type)
			throws MappingException {
		return this.wrapped.getIdentitySelectString(table, column, type);
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
	public String getLimitString(final String query, final int offset, final int limit) {
		return this.wrapped.getLimitString(query, offset, limit);
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
	public Class<?> getNativeIdentifierGeneratorClass() {
		return this.wrapped.getNativeIdentifierGeneratorClass();
	}

	@Override
	public String getNoColumnsInsertString() {
		return this.wrapped.getNoColumnsInsertString();
	}

	@Override
	public String getNotExpression(final String expression) {
		return this.wrapped.getNotExpression(expression);
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
	public String getSelectClauseNullString(final int sqlType) {
		return this.wrapped.getSelectClauseNullString(sqlType);
	}

	@Override
	public String getSelectGUIDString() {
		return this.wrapped.getSelectGUIDString();
	}

	@Override
	public String getSelectSequenceNextValString(final String sequenceName) throws MappingException {
		return this.wrapped.getSelectSequenceNextValString(sequenceName);
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return this.wrapped.getSequenceInformationExtractor();
	}

	@Override
	public String getSequenceNextValString(final String sequenceName) throws MappingException {
		return this.wrapped.getSequenceNextValString(sequenceName);
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
	public String getTypeName(final int code) throws HibernateException {
		return this.wrapped.getTypeName(code);
	}

	@Override
	public String getTypeName(final int code, final long length, final int precision, final int scale)
			throws HibernateException {
		return this.wrapped.getTypeName(code, length, precision, scale);
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return this.wrapped.getUniqueDelegate();
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return this.wrapped.getViolatedConstraintNameExtracter();
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
	public boolean hasDataTypeInIdentityColumn() {
		return this.wrapped.hasDataTypeInIdentityColumn();
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
	public SqlTypeDescriptor remapSqlTypeDescriptor(final SqlTypeDescriptor sqlTypeDescriptor) {
		return this.wrapped.remapSqlTypeDescriptor(sqlTypeDescriptor);
	}

	@Override
	public String renderOrderByElement(final String expression, final String collation, final String order,
			final NullPrecedence nulls) {
		return this.wrapped.renderOrderByElement(expression, collation, order, nulls);
	}

	@Override
	public boolean replaceResultVariableInOrderByClauseWithPosition() {
		return this.wrapped.replaceResultVariableInOrderByClauseWithPosition();
	}

	@Override
	public boolean requiresCastingOfParametersInSelectClause() {
		return this.wrapped.requiresCastingOfParametersInSelectClause();
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
	public boolean supportsEmptyInList() {
		return this.wrapped.supportsEmptyInList();
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
	public boolean supportsIdentityColumns() {
		return this.wrapped.supportsIdentityColumns();
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
	public boolean supportsInsertSelectIdentity() {
		return this.wrapped.supportsInsertSelectIdentity();
	}

	@Override
	public boolean supportsLimit() {
		return this.wrapped.supportsLimit();
	}

	@Override
	public boolean supportsLimitOffset() {
		return this.wrapped.supportsLimitOffset();
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return this.wrapped.supportsLobValueChangePropogation();
	}

	@Override
	public boolean supportsLockTimeouts() {
		return this.wrapped.supportsLockTimeouts();
	}

	@Override
	public boolean supportsNotNullUnique() {
		return this.wrapped.supportsNotNullUnique();
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
	public boolean supportsPooledSequences() {
		return this.wrapped.supportsPooledSequences();
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return this.wrapped.supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return this.wrapped.supportsRowValueConstructorSyntax();
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return this.wrapped.supportsRowValueConstructorSyntaxInInList();
	}

	@Override
	public boolean supportsSequences() {
		return this.wrapped.supportsSequences();
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
	public boolean supportsTuplesInSubqueries() {
		return this.wrapped.supportsTuplesInSubqueries();
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
	public boolean supportsUnique() {
		return this.wrapped.supportsUnique();
	}

	@Override
	public boolean supportsUniqueConstraintInCreateAlterTable() {
		return this.wrapped.supportsUniqueConstraintInCreateAlterTable();
	}

	@Override
	public boolean supportsVariableLimit() {
		return this.wrapped.supportsVariableLimit();
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
	public boolean useFollowOnLocking() {
		return this.wrapped.useFollowOnLocking();
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return this.wrapped.useInputStreamToInsertBlob();
	}

	@Override
	public boolean useMaxForLimit() {
		return this.wrapped.useMaxForLimit();
	}

}
