package org.fastnate.generator.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;

import org.fastnate.generator.converter.EntityConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.StatementsWriter;

import lombok.Getter;

/**
 * Describes a property of an {@link EntityClass} that is a {@link Map}.
 *
 * @author Tobias Liefke
 * @param <E>
 *            The type of the container entity
 * @param <K>
 *            The type of the key of the map
 * @param <T>
 *            The type of the entity inside of the collection
 */
@Getter
public class MapProperty<E, K, T> extends PluralProperty<E, Map<K, T>, T> {

	private static GeneratorColumn buildKeyColumn(final GeneratorTable table, final MapKeyColumn keyColumn,
			final String defaultKeyColumn) {
		if (keyColumn != null && keyColumn.name().length() > 0) {
			return table.resolveColumn(keyColumn.name());
		}
		return table.resolveColumn(defaultKeyColumn);
	}

	private static GeneratorColumn buildKeyColumn(final GeneratorTable table, final MapKeyJoinColumn keyColumn,
			final String defaultKeyColumn) {
		if (keyColumn != null && keyColumn.name().length() > 0) {
			return table.resolveColumn(keyColumn.name());
		}
		return table.resolveColumn(defaultKeyColumn);
	}

	/**
	 * Indicates that the given attribute references a map and may be used by an {@link MapProperty}.
	 *
	 * @param attribute
	 *            the attribute to check
	 * @return {@code true} if an {@link MapProperty} may be created for the given attribute
	 */
	static boolean isMapProperty(final AttributeAccessor attribute) {
		return Map.class.isAssignableFrom(attribute.getType()) && hasPluralAnnotation(attribute);
	}

	/** The class of the key of the map. */
	private final Class<K> keyClass;

	/** The description of the key class, {@code null} if not an entity. */
	private final EntityClass<K> keyEntityClass;

	/** The converter for the target value, {@code null} if not a primitive value. */
	private final ValueConverter<K> keyConverter;

	/** The name of the column that contains the key. */
	private final GeneratorColumn keyColumn;

	/**
	 * Creates a new map property.
	 *
	 * @param sourceClass
	 *            the description of the current inspected class that contains this property
	 * @param attribute
	 *            the accessor of the represented attribute
	 * @param associationOverride
	 *            the configured assocation override
	 * @param attributeOverride
	 *            the configured attribute override, if we reference an {@link ElementCollection}
	 */
	public MapProperty(final EntityClass<?> sourceClass, final AttributeAccessor attribute,
			final AssociationOverride associationOverride, final AttributeOverride attributeOverride) {
		super(sourceClass, attribute, associationOverride, attributeOverride, 1);

		// Initialize the key description
		final MapKeyClass keyClassAnnotation = attribute.getAnnotation(MapKeyClass.class);
		this.keyClass = getPropertyArgument(attribute,
				keyClassAnnotation != null ? keyClassAnnotation.value() : (Class<K>) void.class, 0);
		this.keyEntityClass = sourceClass.getContext().getDescription(this.keyClass);

		if (getMappedBy() != null) {
			this.keyConverter = null;
			this.keyColumn = null;
		} else {
			if (this.keyEntityClass != null) {
				// Entity key
				this.keyConverter = null;
				this.keyColumn = buildKeyColumn(getTable(), attribute.getAnnotation(MapKeyJoinColumn.class),
						attribute.getName() + "_KEY");
			} else {
				// Primitive key
				this.keyConverter = sourceClass.getContext().getProvider().createConverter(attribute, this.keyClass,
						true);
				this.keyColumn = buildKeyColumn(getTable(), attribute.getAnnotation(MapKeyColumn.class),
						attribute.getName() + "_KEY");
			}
		}
	}

	@Override
	public void createPostInsertStatements(final StatementsWriter writer, final E entity) throws IOException {
		if (getMappedBy() == null) {
			final ColumnExpression sourceId = EntityConverter.getEntityReference(entity, getMappedId(), getContext(),
					false);
			for (final Map.Entry<K, T> entry : getValue(entity).entrySet()) {
				final ColumnExpression key;
				if (entry.getKey() == null) {
					key = PrimitiveColumnExpression.NULL;
				} else if (this.keyEntityClass != null) {
					key = EntityConverter.getEntityReference(entry.getKey(), getMappedId(), getContext(), false);
				} else {
					key = this.keyConverter.getExpression(entry.getKey(), getContext());
				}

				createValueStatement(writer, entity, sourceId, key, entry.getValue());
			}
		}
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		Collection<?> keyEntities = Collections.emptyList();
		Collection<?> valueEntities = Collections.emptyList();
		if (this.keyEntityClass != null) {
			keyEntities = getValue(entity).keySet();
		}
		if (isEntityReference()) {
			valueEntities = getValue(entity).values();
		} else if (isEmbedded()) {
			final ArrayList<Object> entities = new ArrayList<>();
			for (final T value : getValue(entity).values()) {
				for (final Property<T, ?> property : getEmbeddedProperties()) {
					entities.addAll(property.findReferencedEntities(value));
				}
			}
			valueEntities = entities;
		}
		if (keyEntities.isEmpty()) {
			return valueEntities;
		} else if (valueEntities.isEmpty()) {
			return keyEntities;
		}
		final ArrayList<Object> entities = new ArrayList<>(keyEntities.size() + valueEntities.size());
		entities.addAll(keyEntities);
		entities.addAll(valueEntities);
		return entities;
	}

	@Override
	public Map<K, T> getValue(final E entity) {
		final Map<K, T> value = super.getValue(entity);
		return value == null ? Collections.EMPTY_MAP : value;
	}

}
