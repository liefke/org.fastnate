package org.fastnate.generator;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fastnate.generator.context.EmbeddedProperty;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratedIdProperty;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.Property;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

import com.google.common.io.Closeables;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Creates SQL statements for a set of entities using their JPA annotations.
 *
 * Known limitations:
 * <ul>
 * <li>Not all databases covered, see {@link GeneratorDialect}.</li>
 * <li>Only tested with Hibernate.</li>
 * </ul>
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class EntitySqlGenerator implements Closeable {

	private static <E> boolean isPostponedInsert(final List<Object> postInsertEntities, final E entity) {
		final int index = postInsertEntities.indexOf(entity);

		// Were we already here, but are required by another entity ?
		final boolean isPostInsert = index >= 0;
		if (isPostInsert && index == postInsertEntities.size() - 1) {
			// We have a required dependency in an endless loop
			throw new IllegalArgumentException("An entity requires another entity that itself requires the first one.");
		}
		return isPostInsert;
	}

	/** Used to write the SQL statements. */
	@Getter
	private final Writer writer;

	/** Used to keep the statement of indices and to remember the database dialect. */
	@Getter
	private final GeneratorContext context;

	/**
	 * Creates a new instance of this {@link EntitySqlGenerator}.
	 *
	 * @param writer
	 *            the writer of the file to generate
	 */
	public EntitySqlGenerator(final Writer writer) {
		this(writer, new GeneratorContext());
	}

	/**
	 * Writes any missing SQL and closes the target writer.
	 *
	 * @throws IOException
	 *             if the target writer throws one in {@link Writer#close()}.
	 */
	@Override
	public void close() throws IOException {
		Closeables.close(this.writer, false);
	}

	/**
	 * Tries to find an entity in an online store. Usefull for writing online updates.
	 *
	 * @param entity
	 *            the entity to check
	 * @return {@code true} if the entity was found and has an id now
	 * @throws IOException
	 *             if something wents wrong
	 */
	protected <E> boolean findEntity(final E entity) throws IOException {
		return false;
	}

	/**
	 * Marks a set of entity references, where we don't know the ID in the database. As we write every unknown entity to
	 * the SQL file, if it is referenced by one that is just written, we need to mark entities that exist already in the
	 * database (and where we possibly have no knowledge of the id of the entity).
	 *
	 * @param entities
	 *            the entity to ignore during {@link #write(Object)}
	 * @throws IOException
	 *             if the writer throws one
	 */
	public <E> void markExistingEntities(final Iterable<E> entities) throws IOException {
		for (final E entity : entities) {
			this.context.getDescription(entity).markExistingEntity(entity);
		}
	}

	/**
	 * Marks an entity reference, where we know the ID in the database. As we write every unknown entity to the SQL
	 * file, if it is referenced by one that is just written, we need to mark entities that exist already in the
	 * database. In difference to {@link #markExistingEntities(Iterable)}, we know the id for the given entity.
	 *
	 * @param entity
	 *            the entity to ignore during {@link #write(Object)}
	 * @param id
	 *            the id of the entity in
	 */
	public <E> void markExistingEntity(final E entity, final Number id) {
		((GeneratedIdProperty<E>) this.context.getDescription(entity).getIdProperty()).markReference(entity,
				id.longValue());
	}

	/**
	 * Creates the Import-SQL for an entity. If the entity was already written (has an id), it is updated (if
	 * nessecary).
	 *
	 * @param entity
	 *            the entity to create the SQL for
	 * @throws IOException
	 *             if the writer throws one
	 * @throws IllegalArgumentException
	 *             if the entity is invalid
	 */
	public <E> void write(final E entity) throws IOException {
		write(entity, new ArrayList<>());
	}

	/**
	 * Creates the Import-SQL for an entity. If the entity was already written, nothing happens.
	 *
	 * @param entity
	 *            the entity to create the SQL for
	 * @param postponedEntities
	 *            contains entities that will be written later and can be ignored
	 * @throws IOException
	 *             if the writer throws one
	 */
	private <E> void write(final E entity, final List<Object> postponedEntities) throws IOException {
		final EntityClass<E> classDescription = this.context.getDescription(entity);
		if (classDescription.isNew(entity)) {
			if (!findEntity(entity) && !isPostponedInsert(postponedEntities, entity)) {
				// We are a new entity that is written later
				postponedEntities.add(entity);
			}

			// Write all contained entities that are mapped in our table(s), as far as possible
			writeTableEntities(entity, postponedEntities, classDescription.getAllProperties());

			// Check if we still need to be created
			if (postponedEntities.remove(entity)) {
				writeInserts(entity, postponedEntities, classDescription, classDescription.getDiscriminator());
			}
		}

	}

	/**
	 * Creates the SQL for the given entities.
	 *
	 * @param entities
	 *            the entities for SQL creation
	 * @throws IOException
	 *             if the writer throws one
	 */
	public <E> void write(final Iterable<? extends E> entities) throws IOException {
		for (final E entity : entities) {
			write(entity);
		}
	}

	/**
	 * Writes a SQL comment to the associated writer.
	 *
	 * @param comment
	 *            the comment to write
	 * @throws IOException
	 *             if thew writer throws one
	 */
	public void writeComment(final String comment) throws IOException {
		this.writer.write("/* " + comment + " */\n");
	}

	private <E> void writeInserts(final E entity, final List<Object> postponedEntities,
			final EntityClass<E> classDescription, final String discriminator) throws IOException {
		// Create the insert statement
		final InsertStatement stmt = new InsertStatement(classDescription.getTable());

		if (classDescription.getJoinedParentClass() != null) {
			// Write the parent tables
			writeInserts(entity, postponedEntities, classDescription.getJoinedParentClass(), discriminator);

			// And add the id as foreign key column
			stmt.addValue(classDescription.getPrimaryKeyJoinColumn(),
					classDescription.getEntityReference(entity, null, false));
		} else {
			// Write Pre-Inserts for the ID
			writeStatements(classDescription.getIdProperty().createPreInsertStatements(entity));

			// Add the id
			classDescription.getIdProperty().addInsertExpression(entity, stmt);

			// And the discriminator
			if (discriminator != null) {
				stmt.addValue(classDescription.getDiscriminatorColumn(), discriminator);
			}
		}

		// Now add all other properties
		for (final Property<E, ?> property : classDescription.getProperties().values()) {
			writeStatements(property.createPreInsertStatements(entity));

			property.addInsertExpression(entity, stmt);
		}

		// Write the statement
		writeStatement(stmt);

		// And all postponed statements
		writeStatements(classDescription.createPostInsertStatements(entity));

		for (final Property<E, ?> property : classDescription.getProperties().values()) {
			// Write all missing entities, even those that have no column (because they are referencing us and
			// we are created now)
			for (final Object referencedEntity : property.findReferencedEntities(entity)) {
				if (!postponedEntities.contains(referencedEntity)) {
					write(referencedEntity, postponedEntities);
				}
			}

			// Generate additional statements
			writeStatements(property.createPostInsertStatements(entity));
		}
	}

	/**
	 * Writes the given statement to the {@link #writer}. May be overridden, if the statements should be written
	 * somewhere else (e.g. directly into a database).
	 *
	 * @param stmt
	 *            the SQL statement to write
	 * @throws IOException
	 *             if the writer throws one
	 */
	protected void writeStatement(final EntityStatement stmt) throws IOException {
		this.writer.write(this.context.getDialect().createSql(stmt));
	}

	private void writeStatements(final List<EntityStatement> statements) throws IOException {
		for (final EntityStatement stmt : statements) {
			writeStatement(stmt);
		}
	}

	private <E, T> void writeTableEntities(final E entity, final List<Object> postponedEntities,
			final Collection<Property<? super E, ?>> properties) throws IOException {
		for (final Property<? super E, ?> property : properties) {
			if (property instanceof EmbeddedProperty) {
				final EmbeddedProperty<? super E, T> embeddedProperty = (EmbeddedProperty<? super E, T>) property;
				this.<T, Object> writeTableEntities(embeddedProperty.getValue(entity), postponedEntities,
						embeddedProperty.getEmbeddedProperties().values());
			} else if (property.isTableColumn()) {
				for (final Object value : property.findReferencedEntities(entity)) {
					if (!postponedEntities.contains(value) || property.isRequired()) {
						write(value, postponedEntities);
					}
				}
			}
		}
	}

}
