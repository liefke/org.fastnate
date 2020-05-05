package org.fastnate.generator.statements;

import org.fastnate.generator.context.SequenceIdGenerator;
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
	 * @param sequence
	 *            describes the sequence
	 * @param difference
	 *            the difference of the referenced ID to the sequence value
	 */
	public CurrentSequenceValueExpression(final SequenceIdGenerator sequence, final long difference) {
		super(sequence, difference);
	}

	@Override
	public String toSql() {
		return toSql(getSequence().getDialect().buildCurrentSequenceValue(getSequence().getQualifiedName(),
				getSequence().getAllocationSize()));
	}

}
