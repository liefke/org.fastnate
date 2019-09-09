package org.fastnate.data.properties;

import java.util.function.Function;

/**
 * Converts a string from an import file to a {@link Character}.
 *
 * @author Tobias Liefke
 */
public class CharacterConverter implements PropertyConverter<Character>, Function<String, Character> {

	@Override
	public Character apply(final String value) {
		return convert(Character.class, value);
	}

	@Override
	public Character convert(final Class<? extends Character> targetType, final String value) {
		if (value == null || value.length() == 0) {
			return targetType == char.class ? ' ' : null;
		}
		return value.charAt(0);
	}

}