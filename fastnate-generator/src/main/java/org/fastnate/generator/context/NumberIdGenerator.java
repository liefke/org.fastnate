package org.fastnate.generator.context;

import org.fastnate.util.ClassUtil;

/**
 * An {@link IdGenerator} that generates incrementing numeric values.
 *
 * @author Tobias Liefke
 */
public abstract class NumberIdGenerator extends IdGenerator<Number> {

	/**
	 * Resolves the next value of this generator.
	 *
	 * @return the generated value
	 */
	protected abstract long createNextValue();

	/**
	 * Resolves the next value of this generator.
	 *
	 * @param propertyClass
	 *            the type of the generated value
	 *
	 * @return the generated value
	 */
	@Override
	public Number createNextValue(final Class<Number> propertyClass) {
		return ClassUtil.convertNumber(createNextValue(), propertyClass);
	}

	/**
	 * The last value returned by {@link #createNextValue(Class)}.
	 *
	 * @return the current value
	 */
	public abstract long getCurrentValue();

	/**
	 * Sets a new start value.
	 *
	 * @param currentValue
	 *            the current value - most likely as extracted from the target database
	 */
	public abstract void setCurrentValue(long currentValue);

}
