package org.fastnate.generator.test;

import jakarta.persistence.GenerationType;

import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/**
 * A Hibernate DialectResolver which allows {@link GenerationType#IDENTITY} in model, even if the database does not
 * support this.
 *
 * @author Tobias Liefke
 */
public class AllowMissingIdentitySupportDialectResolver implements DialectResolver {

    private static final long serialVersionUID = 1L;

    @Override
    public Dialect resolveDialect(final DialectResolutionInfo info) {
        Dialect result = null;

        for (Database database : Database.values()) {
            if (database.matchesResolutionInfo(info)) {
                result = database.createDialect(info);
                break;
            }
        }

        return new AllowMissingIdentitySupportDialect(result);
    }

}
