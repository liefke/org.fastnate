package org.fastnate.generator.statements;

import org.fastnate.generator.dialect.GeneratorDialect;

/**
 * A {@link ColumnExpression} which builds the {@link GeneratorDialect#buildNextSequenceValue(String, int) next value of
 * a sequence}.
 *
 * @author Tobias Liefke
 */
public class NextSequenceValueExpression extends SequenceValueExpression {

	/**
	 * Creates a new instance of an expression that references an ID by using the next value of a sequence.
	 *
	 * @param dialect
	 *            the dialect of the current database
	 * @param sequenceName
	 *            the name of the sequence
	 * @param incrementSize
	 *            the expected incrementSize, as given in the schema - used by some dialects to ensure that exactly that
	 *            inrement is used.
	 * @param difference
	 *            the difference of the referenced ID to the sequence value
	 */
	public NextSequenceValueExpression(final GeneratorDialect dialect, final String sequenceName,
			final int incrementSize, final long difference) {
		super(dialect, sequenceName, incrementSize, difference);
	}

	@Override
	public String toSql() {
		return toSql(getDialect().buildNextSequenceValue(getSequenceName(), getIncrementSize()));
	}

}
