package org.fastnate.data.csv;

/**
 * Converts a string in a CSV file to a {@link Boolean}.
 *
 * @author Tobias Liefke
 */
public final class CsvBooleanConverter implements CsvPropertyConverter<Boolean> {

	@Override
	public Boolean convert(final Class<? extends Boolean> targetType, final String value) {
		return value != null && value.matches("(?i)true|1|on|[yj].*|x");
	}
}