package org.fastnate.generator.context;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

import lombok.Getter;

/**
 * Base class for the description of properties of an {@link EntityClass}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container class
 * @param <T>
 *            The type of the value of the property
 */
@Getter
public abstract class Property<E, T> {

	/** Used to access the value of the property. */
	private final AttributeAccessor attribute;

	/**
	 * Creates a new instance of a property.
	 *
	 * @param attribute
	 *            the accessor of the target attribute
	 */
	protected Property(final AttributeAccessor attribute) {
		this.attribute = attribute;
	}

	/**
	 * Adds an expression according to the current value of the property for the given entity to an SQL insert
	 * statement.
	 *
	 * @param entity
	 *            the inspected entity
	 * @param statement
	 *            the created statement
	 */
	public void addInsertExpression(final E entity, final InsertStatement statement) {
		// The default does nothing
	}

	/**
	 * Creates additional SQL statements that are necessary after this property is written (e.g. for mapping tables) .
	 *
	 * @param entity
	 *            the inspected entity
	 * @return the list of addition insert statements
	 */
	public List<EntityStatement> createPostInsertStatements(final E entity) {
		return Collections.emptyList();
	}

	/**
	 * Creates SQL statements that are necessary before this property is written (e.g. for updating the table
	 * generator).
	 *
	 * @param entity
	 *            the inspected entity
	 * @return the list of addition statements
	 */
	public List<? extends EntityStatement> createPreInsertStatements(final E entity) {
		return Collections.emptyList();
	}

	/**
	 * Throws an IllegalArgumentException if the field was not filled.
	 *
	 * @param entity
	 *            the entity that does not contain a value for this property
	 */
	protected void failIfRequired(final E entity) {
		if (isRequired()) {
			try {
				throw new IllegalArgumentException(
						"Required property " + this + " was not set for " + entity.toString());
			} catch (final NullPointerException e) {
				// It may happen, that the required property is necessary for generation of the toString as well
				throw new IllegalArgumentException("Required property " + this + " was not set");
			}
		}
	}

	/**
	 * Finds all entities in the current property, that are referenced.
	 *
	 * @param entity
	 *            the inspected entity
	 * @return all referenced entities
	 */
	public Collection<?> findReferencedEntities(final E entity) {
		return Collections.emptySet();
	}

	/**
	 * Generates the update statements for an entity that are required after another entity was generated.
	 *
	 * Only called, if {@link EntityClass#markPendingUpdates} was called before for {@code writtenEntity}
	 *
	 * @param entity
	 *            the entity that needs to be updated
	 * @param writtenEntity
	 *            the entity that exists now in the database
	 * @param arguments
	 *            additional arguments that where given to markPendingUpdates
	 * @return the list of pending statements
	 */
	public List<EntityStatement> generatePendingStatements(final E entity, final Object writtenEntity,
			final Object... arguments) {
		return Collections.emptyList();
	}

	/**
	 * Creates the expression for the current value of the given entity in SQL.
	 *
	 * @param entity
	 *            the entity
	 * @param whereExpression
	 *            indicates that the expression is used in a "where" statement
	 * @return the expression for the value of this property or {@code null} if no exists
	 */
	public String getExpression(final E entity, final boolean whereExpression) {
		return null;
	}

	/**
	 * The name of this property.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.attribute.getName();

	}

	/**
	 * Creates an SQL predicate that references all entities that have the same value as the given entity.
	 *
	 * Used to reference entites by their (unique) properties or ids.
	 *
	 * @param entity
	 *            the entity
	 * @return the predicate for the value of that entity or {@code null} if no such expression is available
	 */
	public String getPredicate(final E entity) {
		return null;
	}

	/**
	 * Resolves the current value for this property on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @return the value, {@code null} if entity is {@code null}
	 */
	public T getValue(final E entity) {
		return this.attribute.getValue(entity);
	}

	/**
	 * Indicates if this property is an required field in the database (needs to exist when the insert statement is
	 * written).
	 *
	 * @return {@code true} if the field is required
	 */
	public boolean isRequired() {
		return false;
	}

	/**
	 * Indicates that this property maps to a column from the parent table.
	 *
	 * @return {@code true} if {@link #addInsertExpression(Object, InsertStatement)} will add the corresponding value to
	 *         the given statement
	 */
	public abstract boolean isTableColumn();

	/**
	 * Sets a new value for this property on the given entity.
	 *
	 * @param entity
	 *            the entity to inspect
	 * @param value
	 *            the new value
	 */
	protected void setValue(final E entity, final T value) {
		this.attribute.setValue(entity, value);
	}

	@Override
	public String toString() {
		return this.attribute.getName();
	}

}
