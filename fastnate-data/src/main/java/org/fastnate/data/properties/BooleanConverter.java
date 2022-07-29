package org.fastnate.data.properties;

import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Converts a string in an import file to a {@link Boolean}.
 *
 * Possible values that evaluate to {@code true} (matching is case insensitive):
 * <ul>
 * <li>true</li>
 * <li>1</li>
 * <li>on</li>
 * <li>x</li>
 * <li>y* (= yes - English)</li>
 * <li>s* (= sí - Spanish, Italian, sim - Portuguese)</li>
 * <li>o* (= oui - French)</li>
 * <li>j* (= ja - German)</li>
 * <li>д* (= да - Russian)</li>
 * </ul>
 *
 * @author Tobias Liefke
 */
public class BooleanConverter implements PropertyConverter<Boolean>, Function<String, Boolean> {

	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("(?i)true|1|on|x|[ysoj\u0434].*");

	@Override
	public Boolean apply(final String value) {
		return value != null && BOOLEAN_PATTERN.matcher(value.trim()).matches();
	}

	@Override
	public Boolean convert(final Class<? extends Boolean> targetType, final String value) {
		if (StringUtils.isEmpty(value) && targetType == Boolean.class) {
			return null;
		}
		return apply(value);
	}

}