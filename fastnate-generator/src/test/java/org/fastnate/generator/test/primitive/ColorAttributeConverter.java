package org.fastnate.generator.test.primitive;

import java.awt.Color;

import jakarta.persistence.AttributeConverter;

/**
 * Converts a color to an int and vice versa.
 *
 * @author Tobias Liefke
 */
public class ColorAttributeConverter implements AttributeConverter<Color, Integer> {

	@Override
	public Integer convertToDatabaseColumn(final Color color) {
		return color == null ? null : color.getRGB();
	}

	@Override
	public Color convertToEntityAttribute(final Integer rgba) {
		return rgba == null ? null : new Color(rgba, true);
	}

}
