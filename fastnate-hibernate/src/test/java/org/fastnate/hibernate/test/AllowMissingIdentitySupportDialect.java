package org.fastnate.hibernate.test;

import jakarta.persistence.GenerationType;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectDelegateWrapper;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;

import lombok.RequiredArgsConstructor;

/**
 * A Hibernate Dialect which allows {@link GenerationType#IDENTITY} in model, even if the database does not support
 * this.
 *
 * @author Tobias Liefke
 */
public class AllowMissingIdentitySupportDialect extends DialectDelegateWrapper {

	@RequiredArgsConstructor
	@SuppressWarnings("removal")
	private static final class AllowMissingIdentityColumnSupport implements IdentityColumnSupport {

		private final IdentityColumnSupport identityColumnSupport;

		@Override
		public String appendIdentitySelectToInsert(final String insertString) {
			return this.identityColumnSupport.appendIdentitySelectToInsert(insertString);
		}

		@Override
		public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(final EntityPersister persister) {
			return this.identityColumnSupport.buildGetGeneratedKeysDelegate(persister);
		}

		@Override
		public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(final PostInsertIdentityPersister persister,
				final Dialect dialect) {
			return this.identityColumnSupport.buildGetGeneratedKeysDelegate(persister, dialect);
		}

		@Override
		public String getIdentityColumnString(final int type) throws MappingException {
			try {
				return this.identityColumnSupport.getIdentityColumnString(type);
			} catch (final MappingException e) {
				// Ignore
				return "";
			}
		}

		@Override
		public String getIdentityInsertString() {
			return this.identityColumnSupport.getIdentityInsertString();
		}

		@Override
		public String getIdentitySelectString(final String table, final String column, final int type)
				throws MappingException {
			return this.identityColumnSupport.getIdentitySelectString(table, column, type);
		}

		@Override
		public boolean hasDataTypeInIdentityColumn() {
			return this.identityColumnSupport.hasDataTypeInIdentityColumn();
		}

		@Override
		public boolean supportsIdentityColumns() {
			return this.identityColumnSupport.supportsIdentityColumns();
		}

		@Override
		public boolean supportsInsertSelectIdentity() {
			return this.identityColumnSupport.supportsInsertSelectIdentity();
		}
	}

	/**
	 * Generates a new wrapper around an existing dialect.
	 *
	 * @param wrapped
	 *            the current dialect
	 */
	public AllowMissingIdentitySupportDialect(final Dialect wrapped) {
		super(wrapped);
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new AllowMissingIdentityColumnSupport(this.wrapped.getIdentityColumnSupport());
	}

}
