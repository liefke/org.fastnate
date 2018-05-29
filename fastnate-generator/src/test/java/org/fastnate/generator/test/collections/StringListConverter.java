package org.fastnate.generator.test.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;

/**
 * Converter to test {@link Convert} at a String column.
 *
 * @author Tobias Liefke
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	@Override
	public String convertToDatabaseColumn(final List<String> list) {
		return String.join(",", list);
	}

	@Override
	public List<String> convertToEntityAttribute(final String joined) {
		return new ArrayList<>(Arrays.asList(joined.split(",")));
	}

}