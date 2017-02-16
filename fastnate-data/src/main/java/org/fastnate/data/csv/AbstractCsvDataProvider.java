package org.fastnate.data.csv;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.fastnate.data.DataProvider;
import org.fastnate.data.util.ClassUtil;
import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.context.Property;
import org.fastnate.generator.context.SingularProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for converters that create SQL files from CSV files.
 *
 * @author Tobias Liefke
 *
 * @param <E>
 *            The type of the generated objects
 */
public abstract class AbstractCsvDataProvider<E> extends AbstractCsvReader<E> implements DataProvider {

	private static final Map<Class<?>, CsvPropertyConverter<?>> PROPERTY_CONVERTER = new HashMap<>();

	static {
		PROPERTY_CONVERTER.put(Number.class, new CsvNumberConverter());
		PROPERTY_CONVERTER.put(Boolean.class, new CsvBooleanConverter());
		PROPERTY_CONVERTER.put(Date.class, new CsvDateConverter());
		PROPERTY_CONVERTER.put(Enum.class, new CsvEnumConverter());
		PROPERTY_CONVERTER.put(Character.class, new CsvCharacterConverter());
	}

	@Getter
	private final Collection<E> entities = new ArrayList<>();

	private final Map<String, CsvPropertyConverter<?>> columnConverter = new HashMap<>();

	private final Map<String, String> columnProperties = new HashMap<>();

	private final Set<String> ignoredColumns = new HashSet<>();

	/** Indicates to ignore any column that can't be mapped to a property. */
	@Getter
	@Setter
	private boolean ignoreUnknownColumns;

	/**
	 * Initializes the converter from a path.
	 *
	 * @param importPath
	 *            the path to a CSV file or to a directory that contains the *.csv files
	 */
	protected AbstractCsvDataProvider(final File importPath) {
		super(importPath);
	}

	/**
	 * Defines the mapping from a column name to a property name.
	 *
	 * Only necessary, if column name is not equal to property name
	 *
	 * @param column
	 *            the name of the column
	 * @param property
	 *            the name of the property
	 */
	public void addColumnMapping(final String column, final String property) {
		this.columnProperties.put(column, property);
	}

	/**
	 * Registers the converter to use for a specific column.
	 *
	 * Only nessecary, if the default converters are not enough for a specific column.
	 *
	 * @param column
	 *            the name of the column
	 * @param converter
	 *            used to convert that column from CSV to a Java value
	 */
	public void addConverter(final String column, final CsvPropertyConverter<?> converter) {
		this.columnConverter.put(column, converter);
	}

	/**
	 * Ignores a column during import.
	 *
	 * @param column
	 *            the name of the column that is ignored
	 */
	public void addIgnoredColumn(final String column) {
		this.ignoredColumns.add(column);
	}

	/**
	 * Converts a column value to a property value and sets that property for an entity.
	 *
	 * @param entity
	 *            the entity to modify
	 * @param column
	 *            the name of the current column
	 * @param value
	 *            the value of the property (converts the property, if nessecary)
	 * @return {@code true} if an appropriate property was found, {@code false} if a matching property was not found and
	 *         {@code #isIgnoreUnknownColumns()} is {@code true}
	 *
	 * @throws IllegalArgumentException
	 *             if a matching property was not found or the was not converted
	 */
	protected boolean applyColumn(final E entity, final String column, final String value) {
		if (this.ignoredColumns.contains(column)) {
			return false;
		}

		String property = this.columnProperties.get(column);
		if (property == null) {
			property = column;
		}

		try {
			if (applyColumn(entity, column, property, value)) {
				return true;
			}
			if (!this.ignoreUnknownColumns) {
				throw new IllegalArgumentException(
						"Could not find a setter method for '" + property + "' in " + entity.getClass());
			}
			return false;
		} catch (final IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private boolean applyColumn(final E entity, final String column, final String property, final String value)
			throws IllegalAccessException, InvocationTargetException {
		final String setter = "set" + StringUtils.capitalize(property);
		for (final Method method : entity.getClass().getMethods()) {
			final Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length == 1 && !Modifier.isStatic(method.getModifiers())
					&& method.getName().equals(setter)) {

				// Convert and apply to property
				final Class<?> parameterType = parameterTypes[0];
				final Object convertedValue = convertColumn(column, parameterType, value);
				if (convertedValue != null) {
					try {
						method.invoke(entity, convertedValue);
					} catch (final IllegalArgumentException e) {
						throw new IllegalArgumentException("Could not use " + convertedValue + " of type "
								+ convertedValue.getClass() + " for property " + property + " of type " + parameterType,
								e);
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Starts the generation of the SQL file from the list of CSV files.
	 */
	@Override
	public void buildEntities() throws IOException {
		this.entities.addAll(readImportFiles());
	}

	/**
	 * Tries to convert the given string to a value of the given type for the given column.
	 *
	 * @param column
	 *            the name of the column
	 * @param targetType
	 *            the target type
	 * @param value
	 *            the string representation of the value
	 * @return the value for the property
	 */
	@SuppressWarnings("unchecked")
	protected <T> T convertColumn(final String column, final Class<T> targetType, final String value) {
		CsvPropertyConverter<? super T> converter = (CsvPropertyConverter<T>) this.columnConverter.get(column);
		if (converter == null) {
			if (String.class == targetType) {
				return (T) value;
			}

			converter = findConverter(targetType);
			if (converter == null) {
				throw new IllegalArgumentException("Could not find a converter for " + targetType);
			}
		}

		return (T) converter.convert(targetType, value);
	}

	/**
	 * Builds one or more entities from the given row.
	 *
	 * @param row
	 *            contains the mapping from the header names to the current row data
	 * @return the list of entities from that row
	 */
	@Override
	protected Collection<? extends E> createEntities(final Map<String, String> row) {
		return Collections.singleton(createEntity(row));
	}

	/**
	 * Creates a new empty entity for the current converter.
	 *
	 * Used by the default implementation of {@link #createEntities(Map)}.
	 *
	 * @return the new entity
	 */
	@SuppressWarnings("unchecked")
	protected E createEntity() {
		try {
			return getEntityClass().getConstructor().newInstance();
		} catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		} catch (final NoSuchMethodException e) {
			throw new ModelException("Could not find public no-arg constructor for " + getEntityClass(), e);
		}
	}

	/**
	 * Builds one entity from the given row.
	 *
	 * @param row
	 *            contains the mapping from the header names to the current row data
	 * @return the created and filled entity
	 */
	protected E createEntity(final Map<String, String> row) {
		final E entity = createEntity();
		for (final Map.Entry<String, String> column : row.entrySet()) {
			applyColumn(entity, column.getKey(), column.getValue());
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	private <T> CsvPropertyConverter<? super T> findConverter(final Class<T> targetType) {
		if (targetType == null) {
			return null;
		}
		if (targetType.isPrimitive()) {
			return findConverter(ClassUtils.primitiveToWrapper(targetType));
		}
		CsvPropertyConverter<? super T> converter = (CsvPropertyConverter<? super T>) PROPERTY_CONVERTER
				.get(targetType);
		if (converter == null) {
			converter = findConverter(targetType.getSuperclass());
			if (converter == null) {
				for (final Class<?> interf : targetType.getInterfaces()) {
					converter = findConverter((Class<? super T>) interf);
					if (converter != null) {
						return converter;
					}
				}
			}
		}
		return converter;
	}

	/**
	 * The class of the created entities.
	 *
	 * @return the entity class
	 */
	protected Class<E> getEntityClass() {
		return ClassUtil.getActualTypeBinding(getClass(), AbstractCsvDataProvider.class, 0);
	}

	/**
	 * Defaults to 0.
	 */
	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * Indicates that the given column is ignored during import.
	 *
	 * @param column
	 *            the name of the column
	 * @return {@code true} if the column is not imported
	 */
	public boolean isIgnoredColumn(final String column) {
		return this.ignoredColumns.contains(column);
	}

	/**
	 * Maps the table columns of the singular properties to the CSV columns.
	 *
	 * Useful if the CSV file is a database export.
	 */
	protected void useTableColumns() {
		final EntityClass<E> description = new GeneratorContext().getDescription(getEntityClass());
		for (final Property<? super E, ?> property : description.getAllProperties()) {
			if (property instanceof SingularProperty) {
				final SingularProperty<?, ?> singularProperty = (SingularProperty<?, ?>) property;
				if (singularProperty.isTableColumn() && singularProperty.getColumn() != null) {
					this.columnProperties.put(singularProperty.getColumn(), singularProperty.getAttribute().getName());
				}
			}
		}
	}

	/**
	 * Writes all created entities.
	 */
	@Override
	public void writeEntities(final EntitySqlGenerator sqlGenerator) throws IOException {
		sqlGenerator.write(this.entities);
	}
}
