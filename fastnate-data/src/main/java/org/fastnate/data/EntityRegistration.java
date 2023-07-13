package org.fastnate.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import jakarta.annotation.Resource;

import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.SingularProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Registers entities by their unique properties to offer them to other data providers.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class EntityRegistration implements DataProvider {

	@Getter
	@NoArgsConstructor
	private static final class InvokeLaterHandle<E> {

		private final List<Consumer<E>> consumer = new ArrayList<>();

	}

	private static final class UniqueKey {

		private static final int HASHCODE_MULTIPLIER = 31;

		@Getter
		private final String[] properties;

		@Getter
		private final Object[] values;

		private final int hashCode;

		/**
		 * Creates a new instance of {@link UniqueKey}.
		 *
		 * @param properties
		 *            the names of the properties that make up the key
		 * @param values
		 *            the values of the properties that make up the key
		 */
		UniqueKey(final String[] properties, final Object[] values) {
			this.properties = properties;
			this.values = values;
			int initialHashCode = 0;
			for (int i = 0; i < properties.length; i++) {
				initialHashCode = initialHashCode * HASHCODE_MULTIPLIER + properties[i].hashCode();
				final Object value = values[i];
				if (value != null) {
					initialHashCode = initialHashCode * HASHCODE_MULTIPLIER + value.hashCode();
				}
			}
			this.hashCode = initialHashCode;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof UniqueKey) {
				final UniqueKey other = (UniqueKey) obj;
				if (other.properties.length == this.properties.length) {
					for (int i = 0; i < this.properties.length; i++) {
						if (!this.properties[i].equals(other.properties[i])) {
							return false;
						}
						final Object value = this.values[i];
						if (value == null ? other.values[i] != null : !value.equals(other.values[i])) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			final StringBuilder result = new StringBuilder();
			for (int i = 0; i < this.properties.length; i++) {
				if (i > 0) {
					result.append(", ");
				}
				result.append(this.properties[i]).append(" = \"").append(this.values[i]).append('"');
			}
			return result.toString();
		}

	}

	/** The generation context contains the description of the entity classes, especially unique properties. */
	@Resource
	private final GeneratorContext context;

	/** Mapping from the type of the entity to the name of the uniqe properties with their values and entities. */
	private final Map<Class<?>, Map<UniqueKey, Object>> entities = new HashMap<>();

	private <E> void addEntity(final Map<UniqueKey, Object> uniqueEntities, final E entity,
			final String[] propertyNames, final Object[] propertyValues) {
		final UniqueKey uniqueKey = new UniqueKey(propertyNames, propertyValues);
		final Object oldValue = uniqueEntities.put(uniqueKey, entity);
		if (oldValue instanceof InvokeLaterHandle) {
			for (final Consumer<E> invoker : ((InvokeLaterHandle<E>) oldValue).getConsumer()) {
				invoker.accept(entity);
			}
		} else if (oldValue != null && !oldValue.equals(entity)) {
			throw new IllegalArgumentException("More than one entity of type \"" + this.context.getDescription(entity)
					+ "\" registered for " + uniqueKey);
		}
	}

	@Override
	public void buildEntities() throws IOException {
		// Nothing to do, the entities are imported by all data providers that make use of this registration
	}

	/**
	 * Find an entity, that was registered with {@link #registerEntity(Object)} before.
	 *
	 * @param entityClass
	 *            the class of the required entity
	 * @param uniqueValue
	 *            the value from the property
	 * @return the found entity or {@code null} if not found
	 * @throws IllegalArgumentException
	 *             if more than one unique property exists
	 */
	public <E> E findEntity(final Class<E> entityClass, final Object uniqueValue) {
		final Map<UniqueKey, Object> uniqueValues = this.entities.get(entityClass);
		if (uniqueValues != null) {
			for (final Entry<UniqueKey, Object> entry : uniqueValues.entrySet()) {
				if (entry.getKey().getProperties().length == 1 && uniqueValue.equals(entry.getKey().getValues()[0])) {
					final Object value = entry.getValue();
					if (!(value instanceof InvokeLaterHandle)) {
						return (E) value;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Find an entity, that was registered with {@link #registerEntity(Object)} before.
	 *
	 * @param entityClass
	 *            the class of the required entity
	 * @param uniqueProperty
	 *            the name of the unique property
	 * @param uniqueValue
	 *            the value of the unique property
	 * @return the found entity or {@code null} if not found
	 */
	public <E> E findEntity(final Class<E> entityClass, final String uniqueProperty, final Object uniqueValue) {
		return findEntity(entityClass, new String[] { uniqueProperty }, new Object[] { uniqueValue });
	}

	/**
	 * Find an entity, that was registered with {@link #registerEntity(Object)} before.
	 *
	 * @param entityClass
	 *            the class of the required entity
	 * @param uniqueProperties
	 *            the names of the set of unique properties
	 * @param uniqueValues
	 *            the values of the set of unique properties
	 * @return the found entity or {@code null} if not found
	 */
	public <E> E findEntity(final Class<E> entityClass, final String[] uniqueProperties, final Object[] uniqueValues) {
		final Map<UniqueKey, Object> uniqueEntites = this.entities.get(entityClass);
		if (uniqueEntites != null) {
			final Object value = uniqueEntites.get(new UniqueKey(uniqueProperties, uniqueValues));
			if (!(value instanceof InvokeLaterHandle)) {
				return (E) value;
			}
		}
		return null;
	}

	/**
	 * Invokes an action on a specific entity as soon as this entity was {@link #registerEntity(Object) registered}.
	 *
	 * Use only if just one unique property exists - otherwise use
	 * {@link #invokeOnEntity(Class, String, Object, Consumer)}.
	 *
	 * @param entityClass
	 *            the class of the required entity
	 * @param uniqueValue
	 *            the value from the unique property
	 * @param invoker
	 *            the action to call on the entity as soon as it is registered
	 * @throws IllegalArgumentException
	 *             if more than one unique property exists
	 */
	public <E> void invokeOnEntity(final Class<E> entityClass, final Object uniqueValue, final Consumer<E> invoker) {
		invokeOnEntity(this.context.getDescription(entityClass), uniqueValue, invoker);
	}

	/**
	 * Invokes an action on a specific entity as soon as this entity was {@link #registerEntity(Object) registered}.
	 *
	 * @param entityClass
	 *            the class of the required entity
	 * @param uniqueProperty
	 *            the name of the unique property
	 * @param uniqueValue
	 *            the value from the unique property
	 * @param invoker
	 *            the action to call on the entity as soon as it is registered
	 */
	public <E> void invokeOnEntity(final Class<E> entityClass, final String uniqueProperty, final Object uniqueValue,
			final Consumer<E> invoker) {
		invokeOnEntity(entityClass, new String[] { uniqueProperty }, new Object[] { uniqueValue }, invoker);
	}

	/**
	 * Invokes an action on a specific entity as soon as this entity was {@link #registerEntity(Object) registered}.
	 *
	 * @param entityClass
	 *            the class of the required entity
	 * @param uniqueProperties
	 *            the names of the set of unique properties
	 * @param uniqueValues
	 *            the values of the set of unique properties
	 * @param invoker
	 *            the action to call on the entity as soon as it is registered
	 */
	public <E> void invokeOnEntity(final Class<E> entityClass, final String[] uniqueProperties,
			final Object[] uniqueValues, final Consumer<E> invoker) {
		final Map<UniqueKey, Object> uniqueEntites = this.entities.computeIfAbsent(entityClass, c -> new HashMap<>());
		final Object entity = uniqueEntites.computeIfAbsent(new UniqueKey(uniqueProperties, uniqueValues),
				v -> new InvokeLaterHandle<>());
		if (entity instanceof InvokeLaterHandle) {
			((InvokeLaterHandle<E>) entity).getConsumer().add(invoker);
		} else {
			invoker.accept((E) entity);
		}
	}

	/**
	 * Invokes an action on a specific entity as soon as this entity was {@link #registerEntity(Object) registered}.
	 *
	 * @param templateEntity
	 *            the template that describes the referenced entity (contains a value in at least one set of unique
	 *            properties)
	 * @param invoker
	 *            the action to call on the entity as soon as it is registered
	 */
	public <E> void invokeOnEntity(final E templateEntity, final Consumer<E> invoker) {
		final EntityClass<E> description = this.context.getDescription(templateEntity);
		OUTER: for (final List<SingularProperty<E, ?>> uniqueProperties : description.getAllUniqueProperties()) {
			final int size = uniqueProperties.size();
			final String[] propertyNames = new String[size];
			final Object[] propertyValues = new Object[size];
			for (int i = 0; i < size; i++) {
				final SingularProperty<E, ?> property = uniqueProperties.get(i);
				final Object value = property.getValue(templateEntity);
				if (value == null) {
					continue OUTER;
				}
				propertyNames[i] = property.getName();
				propertyValues[i] = value;
			}
			invokeOnEntity(description.getEntityClass(), propertyNames, propertyValues, invoker);
			return;
		}
		throw new IllegalArgumentException("No unique property set for entity of type " + description.getEntityClass());
	}

	/**
	 * Invokes an action on a specific entity as soon as this entity was {@link #registerEntity(Object) registered}.
	 *
	 * Use only if just one unique property exists - otherwise use
	 * {@link #invokeOnEntity(Class, String, Object, Consumer)}.
	 *
	 * @param entityClass
	 *            describes the class of the required entity
	 * @param uniqueValue
	 *            the value from the unique property
	 * @param invoker
	 *            the action to call on the entity as soon as it is registered
	 * @throws IllegalArgumentException
	 *             if more than one unique property exists
	 */
	public <E> void invokeOnEntity(final EntityClass<E> entityClass, final Object uniqueValue,
			final Consumer<E> invoker) {
		final List<List<SingularProperty<E, ?>>> uniqueProperties = entityClass.getAllUniqueProperties();
		if (uniqueProperties.isEmpty()) {
			throw new IllegalArgumentException("Found no unique properties for " + entityClass);
		} else if (uniqueProperties.size() != 1 || uniqueProperties.get(0).size() != 1) {
			throw new IllegalArgumentException(
					"Found more than one unique property for " + entityClass + ", please decide which to use.");
		}

		invokeOnEntity(entityClass.getEntityClass(), uniqueProperties.get(0).get(0).getName(), uniqueValue, invoker);
	}

	/**
	 * Registers the entity under all of its unique properties.
	 *
	 * @param entity
	 *            the entity
	 */
	public <E> void registerEntity(final E entity) {
		final EntityClass<E> description = this.context.getDescription(entity);
		final List<List<SingularProperty<E, ?>>> uniquePropertySets = description.getAllUniqueProperties();
		if (!uniquePropertySets.isEmpty()) {
			final Map<UniqueKey, Object> uniqueEntities = this.entities.computeIfAbsent(description.getEntityClass(),
					c -> new HashMap<>());
			for (final List<SingularProperty<E, ?>> uniqueProperties : uniquePropertySets) {
				final int size = uniqueProperties.size();
				final String[] propertyNames = new String[size];
				final Object[] propertyValues = new Object[size];
				for (int i = 0; i < size; i++) {
					final SingularProperty<E, ?> property = uniqueProperties.get(i);
					propertyNames[i] = property.getName();
					propertyValues[i] = property.getValue(entity);
				}
				addEntity(uniqueEntities, entity, propertyNames, propertyValues);
			}
		}
	}

	/**
	 * Registers the entity under the given unique combination of properties.
	 *
	 * @param entity
	 *            the entity to register
	 * @param propertyNames
	 *            the names of the properties that are (in combination) unique
	 * @param propertyValues
	 *            the values of the properties
	 */
	public <E> void registerEntity(final E entity, final String[] propertyNames, final Object[] propertyValues) {
		final EntityClass<E> description = this.context.getDescription(entity);
		final Map<UniqueKey, Object> uniqueEntities = this.entities.computeIfAbsent(description.getEntityClass(),
				c -> new HashMap<>());
		addEntity(uniqueEntities, entity, propertyNames, propertyValues);
	}

	@Override
	public void writeEntities(final EntitySqlGenerator sqlGenerator) throws IOException {
		// Ensure that all required entities are written
		for (final Entry<Class<?>, Map<UniqueKey, Object>> classEntry : this.entities.entrySet()) {
			for (final Entry<UniqueKey, Object> entry : classEntry.getValue().entrySet()) {
				final Object entity = entry.getValue();
				if (entity instanceof InvokeLaterHandle) {
					throw new IllegalStateException(
							"Could not find " + classEntry.getKey().getSimpleName() + " with " + entry.getKey());
				}
				sqlGenerator.write(entity);
			}
		}
	}

}
