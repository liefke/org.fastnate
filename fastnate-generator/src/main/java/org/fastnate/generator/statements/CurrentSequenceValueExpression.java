package org.fastnate.generator.statements;

import org.fastnate.generator.dialect.GeneratorDialect;

/**
 * A {@link ColumnExpression} which references an ID using the
 * {@link GeneratorDialect#buildCurrentSequenceValue(String, int) current value of a sequence}.
 *
 * @author Tobias Liefke
 */
public class CurrentSequenceValueExpression extends SequenceValueExpression {

	/**
	 * Creates a new instance of an expression that references an ID by using the current value of a sequence.
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
	public CurrentSequenceValueExpression(final GeneratorDialect dialect, final String sequenceName,
			final int incrementSize, final long difference) {
		super(dialect, sequenceName, incrementSize, difference);
	}

	@Override
	public String toSql() {
		return toSql(getDialect().buildCurrentSequenceValue(getSequenceName(), getIncrementSize()));
	}

}
