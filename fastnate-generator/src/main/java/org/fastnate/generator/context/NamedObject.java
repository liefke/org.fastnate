package org.fastnate.generator.context;

/**
 * Base class for database schema objects that have a (possibly quoted) name.
 *
 * Depending on the dialect the quoting of the name is changed during generation.
 *
 * @author Tobias Liefke
 */
public abstract class NamedObject {

	/**
	 * Resolves the name of this object, as given by the metamodel.
	 *
	 * @return the name of the object, possibly quoted with '"' or '`'
	 */
	public abstract String getName();

	/**
	 * Resolves the fully qualfified name of this object, as it is used by the dialect.
	 *
	 * @return the name of the object, quoted according to the dialect
	 */
	public abstract String getQualifiedName();

	/**
	 * Removes any quotes, if the object name contains them.
	 *
	 * @return the object name without quotes
	 */
	public String getUnquotedName() {
		return unqoteObjectName(getName());
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}

	/**
	 * Removes any quotes from the given object.
	 *
	 * @param objectName
	 *            the name of the object, as given by the metamodel
	 *
	 * @return the object name without quotes or {@code null} if no object name was given
	 */
	protected String unqoteObjectName(final String objectName) {
		if (objectName == null) {
			return null;
		}
		final char firstChar = objectName.charAt(0);
		final int indexLastChar = objectName.length() - 1;
		if (firstChar == '"') {
			if (objectName.charAt(indexLastChar) == '"') {
				return objectName.substring(1, indexLastChar);
			}
		} else if (firstChar == '`' && objectName.charAt(indexLastChar) == '`') {
			return objectName.substring(1, indexLastChar);
		}
		return objectName;
	}

}
