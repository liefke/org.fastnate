package org.fastnate.generator.context;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Access;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;

import org.fastnate.generator.statements.StatementsWriter;
import org.fastnate.generator.statements.TableStatement;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Handling of properties defined in {@link Embeddable}s.
 *
 * @author Tobias Liefke
 *
 * @param <E>
 *            The type of the container class (the entity or parent embeddable)
 * @param <T>
 *            The type of the embeddable object
 */
@Getter
public class EmbeddedProperty<E, T> extends Property<E, T> {

	/**
	 * The properties within the embedded object.
	 */
	private final Map<String, Property<? super T, ?>> embeddedProperties = new TreeMap<>();

	private final boolean id;

	@Nonnull
	@Getter(AccessLevel.NONE)
	private final Constructor<T> constructor;

	/**
	 * Instantiates a new embedded property.
	 *
	 * @param entityClass
	 *            the surrounding entity class
	 * @param table
	 *            the table that contains the attribute
	 * @param attribute
	 *            the attribute that contains the embedded object
	 * @param surroundingAttributeOverrides
	 *            the attribute overrides of the surrounding entity class or embedded property (if we are nested)
	 * @param surroundingAssociationOverrides
	 *            the association overrides of the surrounding entity class or embedded property (if we are nested)
	 */
	public EmbeddedProperty(final EntityClass<?> entityClass, final GeneratorTable table,
			final AttributeAccessor attribute, final Map<String, AttributeOverride> surroundingAttributeOverrides,
			final Map<String, AssociationOverride> surroundingAssociationOverrides) {
		super(attribute);

		this.id = attribute.isAnnotationPresent(EmbeddedId.class);

		final Class<T> type = (Class<T>) attribute.getType();
		if (!type.isAnnotationPresent(Embeddable.class)) {
			throw new IllegalArgumentException(attribute + " does reference " + type + " which is not embeddable.");
		}

		try {
			// Try to find the noargs constructor
			this.constructor = type.getDeclaredConstructor();
			if (!Modifier.isPublic(this.constructor.getModifiers())) {
				this.constructor.setAccessible(true);
			}
		} catch (final NoSuchMethodException e) {
			throw new ModelException("Could not find constructor without arguments for " + type);
		}

		// Determine the access style
		final AccessStyle accessStyle;
		final Access accessType = type.getAnnotation(Access.class);
		if (accessType != null) {
			accessStyle = AccessStyle.getStyle(accessType.value());
		} else {
			accessStyle = attribute.getAccessStyle();
		}

		final String prefix = attribute.getName() + '.';
		final Map<String, AttributeOverride> attributeOverrides = EntityClass
				.getAttributeOverrides(surroundingAttributeOverrides, prefix, attribute.getElement());
		final Map<String, AssociationOverride> associationOverrides = EntityClass
				.getAccociationOverrides(surroundingAssociationOverrides, prefix, attribute.getElement());
		for (final AttributeAccessor embeddedAttribute : accessStyle.getDeclaredAttributes((Class<Object>) type,
				type)) {
			final Property<T, ?> property = entityClass.buildProperty(table, embeddedAttribute, attributeOverrides,
					associationOverrides);
			if (property != null) {
				this.embeddedProperties.put(embeddedAttribute.getName(), property);
			}
		}
	}

	@Override
	public void addInsertExpression(final TableStatement statement, final E entity) {
		final T value = getValue(entity);
		if (value != null) {
			for (final Property<? super T, ?> property : this.embeddedProperties.values()) {
				property.addInsertExpression(statement, value);
			}
		} else {
			failIfRequired(entity);
		}
	}

	@Override
	public void createPostInsertStatements(final StatementsWriter writer, final E entity) throws IOException {
		final T value = getValue(entity);
		for (final Property<? super T, ?> property : this.embeddedProperties.values()) {
			property.createPostInsertStatements(writer, value);
		}
	}

	@Override
	public void createPreInsertStatements(final StatementsWriter writer, final E entity) throws IOException {
		final T value = getValue(entity);
		for (final Property<? super T, ?> property : this.embeddedProperties.values()) {
			property.createPreInsertStatements(writer, value);
		}
	}

	@Override
	public Collection<?> findReferencedEntities(final E entity) {
		final T value = getValue(entity);
		final Set<Object> result = new HashSet<>();
		for (final Property<? super T, ?> property : this.embeddedProperties.values()) {
			result.addAll(property.findReferencedEntities(value));
		}

		return result;
	}

	/**
	 * Ensures that the property of the given entity is initialized.
	 *
	 * If the property has currently a {@code null} value, the property is assigned with a new object using the noargs
	 * constructor.
	 *
	 * @param entity
	 *            the entity to initialize
	 * @return the instance of the embedded object
	 */
	public T getInitializedValue(final E entity) {
		T value = getValue(entity);
		if (value == null) {
			try {
				value = this.constructor.newInstance();
				setValue(entity, value);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new UnsupportedOperationException(e);
			}
		}
		return value;
	}

	@Override
	public String getPredicate(final E entity) {
		if (this.embeddedProperties.isEmpty()) {
			return null;
		}
		final StringBuilder result = new StringBuilder().append('(');
		final T value = getValue(entity);
		for (final Property<? super T, ?> property : this.embeddedProperties.values()) {
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
		for (final Property<? super T, ?> property : this.embeddedProperties.values()) {
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
