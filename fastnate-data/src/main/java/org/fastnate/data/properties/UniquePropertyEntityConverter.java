package org.fastnate.data.properties;

import org.fastnate.data.EntityRegistration;

import lombok.RequiredArgsConstructor;

/**
 * Uses the {@link EntityRegistration} to resolve entities by a specific unique property.
 *
 * If an entity has exactly one unique property, one can use {@link DefaultEntityConverter} instead.
 *
 * If an entity has no unique property or some other kind of key is used as reference in the CSV files, one can use
 * {@link MapConverter}.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class UniquePropertyEntityConverter implements PropertyConverter<Object> {

	/** Used to find the entities. */
	private final EntityRegistration entityRegistration;

	/** The name of the unique properties. */
	private final String uniqueProperty;

	@Override
	public Object convert(final Class<? extends Object> targetType, final String value) {
		return value == null ? null : this.entityRegistration.findEntity(targetType, this.uniqueProperty, value);
	}

}
