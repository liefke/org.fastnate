package org.fastnate.generator.context;

import java.lang.reflect.Field;

/**
 * Represents a value that is exactly one column in the database.
 * 
 * @author Tobias Liefke
 * 
 * @param <E>
 *            The type of the container class
 * @param <T>
 *            The type of the value of the property
 */
public abstract class SingularProperty<E, T> extends Property<E, T> {

	/**
	 * Creates a new instance of a SingularProperty.
	 * 
	 * @param field
	 *            the represented field
	 */
	public SingularProperty(final Field field) {
		super(field);
	}

	/**
	 * The name of the associated column.
	 * 
	 * @return the column name
	 */
	public abstract String getColumn();

	@Override
	public boolean isTableColumn() {
		return true;
	}

}