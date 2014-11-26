package org.fastnate.data.csv;

/**
 * Converts a string from a CSV file to a {@link Character}.
 *
 * @author Tobias Liefke
 */
public final class CsvCharacterConverter implements CsvPropertyConverter<Character> {

	@Override
	public Character convert(final Class<? extends Character> targetType, final String value) {
		return value == null || value.length() == 0 ? null : value.charAt(0);
	}

}