package org.fastnate.generator;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fastnate.generator.context.EmbeddedProperty;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratedIdProperty;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.Property;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.ConnectedStatementsWriter;
import org.fastnate.generator.statements.FileStatementsWriter;
import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

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

	/** Used to keep the state of indices and to store any configuration. */
	@Getter
	private final GeneratorContext context;

	/** The target of any generated SQL statement, e.g. a file or database. */
	@Getter
	private final StatementsWriter writer;

	/**
	 * Creates a new instance for a database connection.
	 *
	 * @param context
	 *            the current context that stores any indices and configuration
	 * @param connection
	 *            the database connection
	 * @throws SQLException
	 *             if the database is not accessible
	 */
	@SuppressWarnings("resource")
	public EntitySqlGenerator(final GeneratorContext context, final Connection connection) throws SQLException {
		this(context, new ConnectedStatementsWriter(connection, context));
	}

	/**
	 * Creates a new instance for a output writer.
	 *
	 * @param context
	 *            the current context that stores any indices and configuration
	 * @param writer
	 *            the stream for the generated file
	 */
	@SuppressWarnings("resource")
	public EntitySqlGenerator(final GeneratorContext context, final Writer writer) {
		this(context, new FileStatementsWriter(writer));
	}

	/**
	 * Writes any missing SQL and closes any open resources.
	 *
	 * @throws IOException
	 *             if the target writer throws one
	 */
	@Override
	public void close() throws IOException {
		writeAlignmentStatements();
		this.writer.close();
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
	 * Writes any open and alignment statements.
	 *
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void flush() throws IOException {
		writeAlignmentStatements();
		this.writer.flush();
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
	 * Writes all statements that are necessary to align ID generators in the database with the current IDs.
	 *
	 * @throws IOException
	 *             if the target writer throws one
	 */
	public void writeAlignmentStatements() throws IOException {
		this.context.writeAlignmentStatements(this.writer);
	}

	/**
	 * Writes a SQL comment to the target writer.
	 *
	 * Shortcut for {@code getWriter().writeComment(comment)}.
	 *
	 * @param comment
	 *            the comment to write
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void writeComment(final String comment) throws IOException {
		this.writer.writeComment(comment);
	}

	private <E> void writeInserts(final E entity, final List<Object> postponedEntities,
			final EntityClass<E> classDescription, final ColumnExpression discriminator) throws IOException {
		// Create the insert statement
		final TableStatement stmt = this.writer.createInsertStatement(this.context.getDialect(),
				classDescription.getTable());

		if (classDescription.getJoinedParentClass() != null) {
			// Write the parent tables
			writeInserts(entity, postponedEntities, classDescription.getJoinedParentClass(), discriminator);

			// And add the id as foreign key column
			stmt.setColumnValue(classDescription.getPrimaryKeyJoinColumn(),
					classDescription.getEntityReference(entity, null, false));
		} else {
			// Write Pre-Inserts for the ID
			classDescription.getIdProperty().createPreInsertStatements(this.writer, entity);

			// Add the id
			classDescription.getIdProperty().addInsertExpression(stmt, entity);

			// And the discriminator
			if (discriminator != null) {
				stmt.setColumnValue(classDescription.getDiscriminatorColumn(), discriminator);
			}
		}

		// Now add all other properties
		for (final Property<E, ?> property : classDescription.getProperties().values()) {
			property.createPreInsertStatements(this.writer, entity);

			property.addInsertExpression(stmt, entity);
		}

		// Write the statement
		this.writer.writeStatement(stmt);

		// And all postponed statements
		classDescription.createPostInsertStatements(entity, this.writer);

		for (final Property<E, ?> property : classDescription.getProperties().values()) {
			// Write all missing entities, even those that have no column (because they are referencing us and
			// we are created now)
			for (final Object referencedEntity : property.findReferencedEntities(entity)) {
				if (referencedEntity != null && !postponedEntities.contains(referencedEntity)) {
					write(referencedEntity, postponedEntities);
				}
			}

			// Generate additional statements
			property.createPostInsertStatements(this.writer, entity);
		}
	}

	/**
	 * Writes a plain SQL statement to the target writer.
	 *
	 * Shortcut for {@code getWriter().writePlainStatement(getContext().getDialect(), statement)}.
	 *
	 * @param statement
	 *            the statement to write
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void writePlainStatement(final String statement) throws IOException {
		this.writer.writePlainStatement(this.context.getDialect(), statement);
	}

	/**
	 * Writes a new line to the target to separate different sections in the SQL file.
	 *
	 * Shortcut for {@code getWriter().writeSectionSeparator()}.
	 *
	 * @throws IOException
	 *             if the writer throws such an exception
	 */
	public void writeSectionSeparator() throws IOException {
		this.writer.writeSectionSeparator();
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
