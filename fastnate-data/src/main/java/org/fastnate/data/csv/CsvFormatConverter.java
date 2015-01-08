package org.fastnate.data.csv;

import java.text.Format;
import java.text.ParseException;

/**
 * Converts a string from a CSV file to a Java object using a {@link Format}.
 *
 * @param <T>
 *            the type of the returned value
 * @author Tobias Liefke
 */
public class CsvFormatConverter<T> implements CsvPropertyConverter<T> {

	private final Format[] formats;

	/**
	 * Creates a new instance of {@link CsvFormatConverter}.
	 *
	 * @param formats
	 *            the list of formats to apply, the first that throws no {@link ParseException} is used
	 */
	public CsvFormatConverter(final Format... formats) {
		this.formats = formats;
	}

	@Override
	public T convert(final Class<? extends T> targetType, final String value) {
		if (value == null) {
			return null;
		}
		ParseException firstError = null;
		for (final Format format : this.formats) {
			try {
				return (T) format.parseObject(value);
			} catch (final ParseException e) {
				if (firstError == null) {
					firstError = e;
				}
			}
		}
		throw new IllegalArgumentException(value, firstError);
	}
}