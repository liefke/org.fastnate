package org.fastnate.generator.test;

import javax.persistence.GenerationType;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * A Hibernate DialectResolver which allows {@link GenerationType#IDENTITY} in model, even if the database does not
 * support this.
 *
 * @author Tobias Liefke
 */
public class AllowMissingIdentitySupportDialectResolver extends StandardDialectResolver {

	private static final long serialVersionUID = 1L;

	@Override
	public Dialect resolveDialect(final DialectResolutionInfo info) {
		return new AllowMissingIdentitySupportDialect(super.resolveDialect(info));
	}

}
