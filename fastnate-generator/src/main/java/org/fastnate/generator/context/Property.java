package org.fastnate.generator.context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import jakarta.persistence.CascadeType;

import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

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

	/**
	 * Indicates that the property is a composition according to the given cascade types.
	 *
	 * A composition removes the target entity, if the owning entity is removed.
	 *
	 * @param cascades
	 *            the "cascade" attribute of the mapping annotation.
	 * @return {@code true} if we are on the owning side of a composition relationship, {@code false} if we are on the
	 *         child side or if this is an association
	 */
	protected static boolean isComposition(final CascadeType[] cascades) {
		for (final CascadeType cascade : cascades) {
			if (cascade == CascadeType.ALL || cascade == CascadeType.REMOVE) {
				return true;
			}
		}
		return false;
	}

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
	 * @param statement
	 *            the created statement
	 * @param entity
	 *            the inspected entity
	 */
	public void addInsertExpression(final TableStatement statement, final E entity) {
		// The default does nothing
	}

	/**
	 * Creates additional SQL statements that are necessary after this property is written (e.g. for mapping tables).
	 *
	 * @param writer
	 *            the target of the created statements
	 * @param entity
	 *            the inspected entity
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void createPostInsertStatements(final StatementsWriter writer, final E entity) throws IOException {
		// The default property has no post insert statements
	}

	/**
	 * Creates SQL statements that are necessary before this property is written (e.g. for updating the table
	 * generator).
	 *
	 * @param entity
	 *            the inspected entity
	 * @param writer
	 *            the target of the created statements
	 * @throws IOException
	 *             if the writer throws an exception
	 *
	 */
	public void createPreInsertStatements(final StatementsWriter writer, final E entity) throws IOException {
		// The default property has no pre insert statements
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
	 * @param writer
	 *            the target of the generated statements
	 * @param entity
	 *            the entity that needs to be updated
	 * @param writtenEntity
	 *            the entity that exists now in the database
	 * @param arguments
	 *            additional arguments that where given to markPendingUpdates
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void generatePendingStatements(final StatementsWriter writer, final E entity, final Object writtenEntity,
			final Object... arguments) throws IOException {
		// The default property has no pending statements
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
	public ColumnExpression getExpression(final E entity, final boolean whereExpression) {
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
	 * The type of the property.
	 *
	 * @return the {@link AttributeAccessor#getType() type of the attribute}
	 */
	public Class<T> getType() {
		return (Class<T>) this.attribute.getType();
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
	 * @return {@code true} if {@link #addInsertExpression} will add the corresponding value to the given statement
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
	public void setValue(final E entity, final T value) {
		this.attribute.setValue(entity, value);
	}

	@Override
	public String toString() {
		return this.attribute.getName();
	}

}
