package org.fastnate.data.properties;

import org.fastnate.data.EntityRegistration;

import lombok.RequiredArgsConstructor;

/**
 * Uses the {@link EntityRegistration} to resolve entities by their unique property.
 *
 * If an entity has more than one unique property, one should use {@link UniquePropertyEntityConverter} instead.
 *
 * If an entity has no unique property or some other kind of key is used as reference in the CSV files, one can use
 * {@link MapConverter}.
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class DefaultEntityConverter implements PropertyConverter<Object> {

	/** Used to find the entities. */
	private final EntityRegistration entityRegistration;

	@Override
	public Object convert(final Class<? extends Object> targetType, final String value) {
		return value == null ? null : this.entityRegistration.findEntity(targetType, value);
	}

}
