package org.fastnate.data.properties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fastnate.generator.context.CollectionProperty;
import org.fastnate.generator.context.MapProperty;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.context.PluralProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Wraps the contents of a {@link PluralProperty} to use a common interface during import, independent of the actual
 * subtype.
 *
 * @author Tobias Liefke
 * @param <E>
 *            the type of the elements
 */
public abstract class PluralPropertyContents<E> {

	@NoArgsConstructor
	private static final class ListCollection<E> extends PluralPropertyContents<E> {

		@Getter(AccessLevel.PACKAGE)
		private List<E> collection;

		@Override
		public void addElement(final E element) {
			this.collection.add(element);
		}

		@Override
		void create() {
			this.collection = new ArrayList<>();
		}

		@Override
		void setCollection(final Object value) {
			this.collection = (List<E>) value;
		}

		@Override
		public void setElement(final int index, final Object key, final E element) {
			while (this.collection.size() <= index) {
				this.collection.add(null);
				this.collection.set(index, element);
			}
		}

	}

	@NoArgsConstructor
	private static final class MapCollection<E> extends PluralPropertyContents<E> {

		@Getter(AccessLevel.PACKAGE)
		private Map<Object, E> collection;

		@Override
		void create() {
			this.collection = new LinkedHashMap<>();
		}

		@Override
		void setCollection(final Object value) {
			this.collection = (Map<Object, E>) value;
		}

		@Override
		public void setElement(final int index, final Object key, final E element) {
			this.collection.put(key, element);
		}

	}

	@RequiredArgsConstructor
	private static final class SetCollection<E, V> extends PluralPropertyContents<V> {

		@Getter(AccessLevel.PACKAGE)
		private Set<V> collection;

		@Override
		void create() {
			this.collection = new LinkedHashSet<>();
		}

		@Override
		void setCollection(final Object value) {
			this.collection = (Set<V>) value;
		}

		@Override
		public void setElement(final int index, final Object key, final V element) {
			this.collection.add(element);
		}

	}

	/**
	 * Creates a wrapper around the contents of the given property in the given entity.
	 *
	 * If the current property content is empty, it is initialized with a new empty collection resp. map.
	 *
	 * @param entity
	 *            the entity that contains the property
	 * @param property
	 *            the property
	 * @return the wrapper around the contents of the property for the entity
	 */
	public static <E, C, V> PluralPropertyContents<V> create(final E entity, final PluralProperty<E, C, V> property) {
		final PluralPropertyContents<V> collection;
		if (property instanceof MapProperty) {
			collection = new MapCollection<>();
		} else if (property instanceof CollectionProperty) {
			if (List.class.isAssignableFrom(property.getType())) {
				collection = new ListCollection<>();
			} else {
				collection = new SetCollection<>();
			}
		} else {
			throw new ModelException("Can't handle plural property: " + property);
		}
		final Object collectionObject = property.getAttribute().getValue(entity);
		if (collectionObject == null) {
			collection.create();
			property.setValue(entity, (C) collection.getCollection());
		} else {
			collection.setCollection(collectionObject);
		}
		return collection;
	}

	/**
	 * Adds an element to the end of the collection.
	 *
	 * Shouldn't be used for maps, as it uses a "null" key
	 *
	 * @param element
	 *            the element to add to the end of the collection
	 */
	public void addElement(final E element) {
		setElement(0, null, element);
	}

	abstract void create();

	abstract Object getCollection();

	abstract void setCollection(Object value);

	/**
	 * Sets an element in the collection resp. map.
	 *
	 * @param index
	 *            the index of the added element, if the contents is a list
	 * @param key
	 *            the key of the element, if the contents is a map
	 * @param element
	 *            the element to add
	 */
	public abstract void setElement(int index, Object key, E element);

}