package org.fastnate.generator.statements;

import org.fastnate.generator.context.SequenceIdGenerator;
import org.fastnate.generator.dialect.GeneratorDialect;

/**
 * A {@link ColumnExpression} which references an ID using the
 * {@link GeneratorDialect#buildCurrentSequenceValue(String, int, boolean) current value of a sequence}.
 *
 * @author Tobias Liefke
 */
public class CurrentSequenceValueExpression extends SequenceValueExpression {

	private final boolean firstCall;

	/**
	 * Creates a new instance of an expression that references an ID by using the current value of a sequence.
	 *
	 * @param sequence
	 *            describes the sequence
	 * @param difference
	 *            the difference of the referenced ID to the sequence value
	 * @param firstCall
	 *            indicates, that this is the first call to the sequence in this session
	 */
	public CurrentSequenceValueExpression(final SequenceIdGenerator sequence, final long difference,
			final boolean firstCall) {
		super(sequence, difference);
		this.firstCall = firstCall;
	}

	@Override
	public String toSql() {
		return toSql(getSequence().getDialect().buildCurrentSequenceValue(getSequence().getQualifiedName(),
				getSequence().getAllocationSize(), this.firstCall));
	}

}
