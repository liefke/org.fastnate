package org.fastnate.generator.context;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import com.google.common.collect.ImmutableMap;

import lombok.Getter;

/**
 * Contains all known generators and there current values for a {@link GeneratorContext}.
 *
 * @author Tobias Liefke
 */
@Getter
public class GeneratedIds {

	/** Contains the default values for a sequence generator, if none is given. */
	private static final SequenceGenerator DEFAULT_SEQUENCE_GENERATOR = AnnotationDefaults
			.create(SequenceGenerator.class, ImmutableMap.of("allocationSize", 1));

	/** Contains the default values for a table generator, if none is given. */
	private static final TableGenerator DEFAULT_TABLE_GENERATOR = AnnotationDefaults.create(TableGenerator.class);

	/** Mapping from a {@link SequenceGenerator#name()} to the generator itself. */
	private final Map<String, SequenceGenerator> sequenceGenerators = new HashMap<>();

	/** Mapping from a {@link TableGenerator#name()} to the generator itself. */
	private final Map<String, TableGenerator> tableGenerators = new HashMap<>();

	/** Contains the current values for all {@link SequenceGenerator sequences}. */
	private final Map<String, Long> sequenceValues = new HashMap<>();

	/** Contains the current values for all {@link TableGenerator generator tables}. */
	private final Map<String, Long> tableValues = new HashMap<>();

	/** Contains the current values for {@link GeneratedValue ids} with {@link GenerationType#IDENTITY}. */
	private final Map<String, Long> idValues = new HashMap<>();

	/**
	 * Creates a new instance of {@link GeneratedIds}.
	 */
	public GeneratedIds() {
		this.sequenceGenerators.put("", DEFAULT_SEQUENCE_GENERATOR);
		this.tableGenerators.put("", DEFAULT_TABLE_GENERATOR);
	}

	/**
	 * Creates the next value for a sequence (and remembers that value).
	 *
	 * @param generator
	 *            the generator of the current column
	 * @return the created value
	 */
	public Long createNextValue(final SequenceGenerator generator) {
		final String sequenceName = generator.sequenceName();
		final Long currentValue = this.sequenceValues.get(sequenceName);
		final Long newValue;
		if (currentValue == null) {
			newValue = (long) generator.initialValue();
		} else {
			// Allocation size is _not_ necessarily the increment size
			// As soon as we read hibernate specific properties, we can read the increment size as well
			newValue = currentValue + generator.allocationSize();
		}
		this.sequenceValues.put(sequenceName, newValue);
		return newValue;
	}

	/**
	 * Creates the next value for a identity column (and remembers that value).
	 *
	 * @param columnId
	 *            the ID of the column (including table name)
	 * @return the created value
	 */
	public Long createNextValue(final String columnId) {
		final Long currentValue = this.idValues.get(columnId);
		final Long newValue;
		if (currentValue == null) {
			newValue = 0L;
		} else {
			newValue = currentValue + 1;
		}
		this.idValues.put(columnId, newValue);
		return newValue;
	}

	/**
	 * Resolves the current value for a sequence.
	 *
	 * @param generator
	 *            the generator of the current column
	 * @return the current value or {@code null} if the sequence was not used up to now
	 */
	public Long getCurrentValue(final SequenceGenerator generator) {
		return this.sequenceValues.get(generator.sequenceName());
	}

	/**
	 * Resolves the current value for a generated column.
	 *
	 * Used for {@link GenerationType#IDENTITY}
	 *
	 * @param columnId
	 *            the ID of the column (including the table prefix)
	 * @return the current value or {@code null} if no row was created up to now
	 */
	public Long getCurrentValue(final String columnId) {
		final Long currentId = this.idValues.get(columnId);
		if (currentId == null) {
			throw new IllegalArgumentException("No current value for: " + columnId);
		}
		return currentId;
	}

	/**
	 * Registers the {@link TableGenerator} and {@link SequenceGenerator} declared at the given element.
	 *
	 * If neither annotation is present, nothing happens.
	 *
	 * @param element
	 *            the inspected class, method or field
	 */
	public void registerGenerators(final AnnotatedElement element) {
		final SequenceGenerator sequenceGenerator = element.getAnnotation(SequenceGenerator.class);
		if (sequenceGenerator != null) {
			this.sequenceGenerators.put(sequenceGenerator.name(), sequenceGenerator);
		}

		final TableGenerator tableGenerator = element.getAnnotation(TableGenerator.class);
		if (tableGenerator != null) {
			this.tableGenerators.put(tableGenerator.name(), tableGenerator);
		}
	}

}
