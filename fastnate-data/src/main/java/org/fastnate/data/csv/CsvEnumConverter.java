package org.fastnate.data.csv;

/**
 * Converts a string from a CSV file to an Enum value.
 *
 * @author Tobias Liefke
 */
public class CsvEnumConverter implements CsvPropertyConverter<Enum<?>> {

	@Override
	@SuppressWarnings("rawtypes")
	public Enum<?> convert(final Class<? extends Enum<?>> targetType, final String value) {
		return Enum.valueOf((Class) targetType, value.trim());
	}
}