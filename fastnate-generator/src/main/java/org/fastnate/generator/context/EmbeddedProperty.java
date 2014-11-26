package org.fastnate.generator.context;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;

import org.fastnate.generator.statements.EntityStatement;
import org.fastnate.generator.statements.InsertStatement;

import lombok.Getter;

/**
 * Handling of properties defined in {@link Embeddable}s.
 * 
 * @author Tobias Liefke
 * 
 * @param <E>
 *            The type of the container class (the entity)
 * @param <T>
 *            The type of the embeddable object
 */
@Getter
public class EmbeddedProperty<E, T> extends Property<E, T> {

	/**
	 * The properties within the embedded object.
	 */
	private final Map<String, Property<T, ?>> embeddedProperties = new TreeMap<>();

	private final boolean id;

	/**
	 * Instantiates a new embedded property.
	 * 
	 * @param entityClass
	 *            the class of the entity
	 * 
	 * @param entityField
	 *            the field that contains the embedded object
	 */
	public EmbeddedProperty(final EntityClass<?> entityClass, final Field entityField) {
		super(entityField);

		this.id = entityField.getAnnotation(EmbeddedId.class) != null;

		final Class<?> type = entityField.getType();
		if (type.getAnnotation(Embeddable.class) == null) {
			throw new IllegalArgumentException(entityField + " does reference " + type + " which is not embeddable.");
		}
		final Map<String, AttributeOverride> attributeOverrides = EntityClass.getAttributeOverrides(entityField);
		for (final Field field : type.getDeclaredFields()) {
			final AttributeOverride attrOveride = attributeOverrides.get(field.getName());
			final Property<T, ?> property = entityClass.buildProperty(field, attrOveride != null ? attrOveride.column()
					: field.getAnnotation(Column.class),
					EntityClass.getAccociationOverrides(entityField).get(field.getName()));
			if (property != null) {
				this.embeddedProperties.put(field.getName(), property);
			}
		}
	}

	@Override
	public void addInsertExpression(final E entity, final InsertStatement statement) {
		final T value = getValue(entity);
		if (value != null) {
			for (final Property<T, ?> property : this.embeddedProperties.values()) {
				property.addInsertExpression(value, statement);
			}
		} else {
			failIfRequired();
		}
	}

	@Override
	public List<EntityStatement> buildAdditionalStatements(final E entity) {
		final T value = getValue(entity);
		final List<EntityStatement> result = new ArrayList<>();
		for (final Property<T, ?> property : this.embeddedProperties.values()) {
			result.addAll(property.buildAdditionalStatements(value));
		}

		return result;
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		final T value = getValue(entity);
		final Set<Object> result = new HashSet<>();
		for (final Property<T, ?> property : this.embeddedProperties.values()) {
			result.addAll(property.findReferencedEntities(value));
		}

		return result;
	}

	@Override
	public String getPredicate(final E entity) {
		if (this.embeddedProperties.isEmpty()) {
			return null;
		}
		final StringBuilder result = new StringBuilder().append('(');
		final T value = getValue(entity);
		for (final Property<T, ?> property : this.embeddedProperties.values()) {
			if (result.length() > 1) {
				result.append(" AND ");
			}
			result.append(property.getPredicate(value));
		}
		return result.append(")").toString();
	}

	@Override
	public boolean isRequired() {
		if (this.id) {
			return true;
		}
		for (final Property<T, ?> property : this.embeddedProperties.values()) {
			if (property.isRequired()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isTableColumn() {
		return true;
	}

}
