package org.fastnate.data.properties;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

/**
 * Converts a string in an import file file to a date.
 *
 * Contains the most typical date formats from ISO 8601 and some other worldwide used formats.
 *
 * To use a custom date format, see {@link FormatConverter}.
 *
 * @author Tobias Liefke
 * @param <D>
 *            the actual date type
 */
public class DateConverter<D extends Date> extends FormatConverter<D> {

	/**
	 * Creates a new converter with the default formats for the default timezone.
	 */
	public DateConverter() {
		super(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), new SimpleDateFormat("yyyy-MM-dd"), //
				new SimpleDateFormat("'T'HH:mm:ss"), new SimpleDateFormat("HH:mm:ss"), //
				new SimpleDateFormat("dd.MM.yy HH:mm:ss"), new SimpleDateFormat("dd.MM.yy"), //
				new SimpleDateFormat("HH:mm:ss"), new SimpleDateFormat("MM/dd/yy HH:mm:ss"), //
				new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("dd-MMM-yy"));
	}

	/**
	 * Creates a new instance for a specific timezone.
	 *
	 * @param timeZone
	 *            the time zone to use for _all_ default formats
	 */
	public DateConverter(final TimeZone timeZone) {
		this();
		getFormats().forEach(format -> ((SimpleDateFormat) format).setTimeZone(timeZone));
	}

	@Override
	public D convert(final Class<? extends D> targetType, final String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		final Date date = super.convert(targetType, value);
		if (targetType.isInstance(date)) {
			return (D) date;
		}
		try {
			return targetType.getConstructor(long.class).newInstance(date.getTime());
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}
}