package org.fastnate.eclipselink;

import jakarta.persistence.GenerationType;

import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.provider.JpaProvider;

/**
 * Encapsulates implementation details of EclipseLink as JPA provider.
 *
 * @author Tobias Liefke
 */
public class EclipseLinkProvider implements JpaProvider {

	@Override
	public GenerationType getAutoGenerationType(final GeneratorDialect dialect) {
		return GenerationType.TABLE;
	}

	@Override
	public String getDefaultGeneratorTable() {
		return "sequence";
	}

	@Override
	public String getDefaultGeneratorTablePkColumnName() {
		return "seq_name";
	}

	@Override
	public String getDefaultGeneratorTableValueColumnName() {
		return "seq_count";
	}

	@Override
	public String getDefaultSequence(final String tableName) {
		return "seq_gen";
	}

	@Override
	public boolean isJoinedDiscriminatorNeeded() {
		return false;
	}

}
