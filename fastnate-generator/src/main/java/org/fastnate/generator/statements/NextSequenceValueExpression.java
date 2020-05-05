package org.fastnate.generator.statements;

import org.fastnate.generator.context.SequenceIdGenerator;
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
	 * @param sequence
	 *            describes the sequence
	 * @param difference
	 *            the difference of the referenced ID to the sequence value
	 */
	public NextSequenceValueExpression(final SequenceIdGenerator sequence, final long difference) {
		super(sequence, difference);
	}

	@Override
	public String toSql() {
		return toSql(getSequence().getDialect().buildNextSequenceValue(getSequence().getQualifiedName(),
				getSequence().getAllocationSize()));
	}

}
