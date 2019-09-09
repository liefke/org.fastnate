package org.fastnate.data.properties;

import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.fastnate.util.ClassUtil;

import lombok.Getter;

/**
 * Converts a string from an import file to a Java object using a list of {@link Format}s.
 *
 * If the value is a number, the appropriate type conversion is applied.
 *
 * @param <T>
 *            the type of the returned value
 * @author Tobias Liefke
 */
public class FormatConverter<T> implements PropertyConverter<T> {

	/**
	 * The value to use if the column content is empty.
	 */
	private static final Integer DEFAULT_VALUE = Integer.valueOf(0);

	/**
	 * All formats to test.
	 *
	 * The first that throws no {@link ParseException} is used for conversion.
	 */
	@Getter
	private final List<Format> formats = new ArrayList<>();

	/**
	 * Creates a new instance of a {@link FormatConverter}.
	 *
	 * @param formats
	 *            the list of formats to try for conversion, the first that throws no {@link ParseException} is used
	 */
	public FormatConverter(final Format... formats) {
		this.formats.addAll(Arrays.asList(formats));
	}

	@Override
	public T convert(final Class<? extends T> targetType, final String value) {
		if (StringUtils.isBlank(value)) {
			if (targetType.isPrimitive()) {
				return (T) ClassUtil.convertNumber(DEFAULT_VALUE, (Class<? extends Number>) targetType);
			}
			return null;
		}
		ParseException firstError = null;
		for (final Format format : this.formats) {
			try {
				final Object result = format.parseObject(value);
				if (result instanceof Number) {
					// NumberFormat returns a Long or Double, even if we need an Int or Float
					return (T) ClassUtil.convertNumber((Number) result, (Class<? extends Number>) targetType);
				}
				return (T) result;
			} catch (final ParseException e) {
				if (firstError == null) {
					firstError = e;
				}
			}
		}
		throw new IllegalArgumentException(value, firstError);
	}

}