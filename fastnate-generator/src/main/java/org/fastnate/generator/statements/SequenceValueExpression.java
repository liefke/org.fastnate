package org.fastnate.generator.statements;

import org.fastnate.generator.dialect.GeneratorDialect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base class for {@link ColumnExpression}s which reference an ID by using the value of a sequence.
 *
 * @author Tobias Liefke
 */
@Getter
@RequiredArgsConstructor
public abstract class SequenceValueExpression implements ColumnExpression {

	/** The dialect of the current database. */
	private final GeneratorDialect dialect;

	/** The name of the sequence. */
	private final String sequenceName;

	/**
	 * The expected incrementSize, as given in the schema - used by some dialects to ensure that exactly that inrement
	 * is used.
	 */
	private final int incrementSize;

	/** The difference of the referenced ID to the sequence value. */
	private final long difference;

	/**
	 * Builds the SQL expression by using the given SQL and the current {@link #difference}.
	 *
	 * @param sql
	 *            the SQL expression that references the sequence
	 * @return the SQL expression that references the sequence and respects the difference
	 */
	protected String toSql(final String sql) {
		if (this.difference == 0) {
			return sql;
		}
		return '(' + sql + " - " + this.difference + ')';
	}
}
