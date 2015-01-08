package org.fastnate.data.csv;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * Converts a string in a CSV file to a date.
 *
 * Contains already the most typical date formats. To use a custom date format, see {@link CsvFormatConverter}.
 *
 * @author Tobias Liefke
 */
public final class CsvDateConverter extends CsvFormatConverter<Date> {

	/**
	 * Creates a new instance of {@link CsvDateConverter}.
	 */
	public CsvDateConverter() {
		super(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), new SimpleDateFormat("yyyy-MM-dd"), //
				new SimpleDateFormat("'T'HH:mm:ss"), new SimpleDateFormat("HH:mm:ss"), //
				new SimpleDateFormat("dd.MM.yy HH:mm:ss"), new SimpleDateFormat("dd.MM.yy"), //
				new SimpleDateFormat("HH:mm:ss"), new SimpleDateFormat("MM/dd/yy HH:mm:ss"), //
				new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("dd-MMM-yy"));
	}

	@Override
	public Date convert(final Class<? extends Date> targetType, final String value) {
		if (StringUtils.isEmpty(value)) {
			return null;
		}
		final Date date = super.convert(targetType, value);
		try {
			return targetType.getConstructor(long.class).newInstance(date.getTime());
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}
}