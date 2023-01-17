package org.fastnate.generator.context;

import org.fastnate.generator.dialect.GeneratorDialect;

/**
 * Base class for database schema objects that have a (possibly quoted) name.
 *
 * Depending on the dialect the quoting of the name is changed during generation.
 *
 * @author Tobias Liefke
 */
public abstract class NamedObject {

	/**
	 * Resolves the name of this object.
	 *
	 * @return the name of the object, possibly quoted with '"' or '`'
	 */
	public abstract String getName();

	/**
	 * Resolves the name of this column for the given dialect.
	 *
	 * @param dialect
	 *            the current database dialect
	 *
	 * @return the column name with the correct quotes
	 */
	public String getName(final GeneratorDialect dialect) {
		return dialect.adjustObjectName(getName());
	}

	/**
	 * Removes any quotes, if the column name contains them.
	 *
	 * @return the column name without quotes
	 */
	public String getUnquotedName() {
		final String name = getName();
		final char firstChar = name.charAt(0);
		final int indexLastChar = name.length() - 1;
		if (firstChar == '"') {
			if (name.charAt(indexLastChar) == '"') {
				return name.substring(1, indexLastChar);
			}
		} else if (firstChar == '`' && name.charAt(indexLastChar) == '`') {
			return name.substring(1, indexLastChar);
		}
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
