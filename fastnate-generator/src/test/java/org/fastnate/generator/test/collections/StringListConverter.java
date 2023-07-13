package org.fastnate.generator.test.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;

/**
 * Converter to test {@link Convert} at a String column.
 *
 * @author Tobias Liefke
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	@Override
	public String convertToDatabaseColumn(final List<String> list) {
		if (list == null) {
			return null;
		}
		return String.join(",", list);
	}

	@Override
	public List<String> convertToEntityAttribute(final String joined) {
		if (joined == null) {
			return null;
		}
		return new ArrayList<>(Arrays.asList(joined.split(",")));
	}

}