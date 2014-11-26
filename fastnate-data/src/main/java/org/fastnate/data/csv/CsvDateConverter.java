package org.fastnate.data.csv;

import java.lang.reflect.InvocationTargetException;
import java.text.Format;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;

/**
 * Converts a string in a CSV file to a date.
 *
 * Contains already the most typical date formats. To use a custom date format, see {@link CsvFormatConverter}.
 *
 * @author Tobias Liefke
 */
public final class CsvDateConverter implements CsvPropertyConverter<Date> {

	private static final Format[] FORMATTER = new Format[] {
			DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT, DateFormatUtils.ISO_DATETIME_FORMAT,
			FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss"), DateFormatUtils.ISO_DATE_FORMAT,
			DateFormatUtils.ISO_TIME_FORMAT, DateFormatUtils.ISO_TIME_NO_T_FORMAT,
			FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss"), FastDateFormat.getInstance("dd.MM.yyyy"),
			FastDateFormat.getInstance("HH:mm:ss"), FastDateFormat.getInstance("MM/dd/yyyy HH:mm:ss"),
			FastDateFormat.getInstance("MM/dd/yyyy"), FastDateFormat.getInstance("dd-MMM-yy") };

	@Override
	public Date convert(final Class<? extends Date> targetType, final String value) {
		for (final Format format : FORMATTER) {
			final Date date;
			try {
				date = (Date) format.parseObject(value);
			} catch (final ParseException e) {
				continue;
			}
			try {
				return targetType.getConstructor(long.class).newInstance(date.getTime());
			} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
					| NoSuchMethodException e) {
				throw new IllegalArgumentException(e);
			}
		}
		throw new IllegalArgumentException("Not a date: " + value);
	}
}