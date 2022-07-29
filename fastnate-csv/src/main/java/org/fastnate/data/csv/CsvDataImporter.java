package org.fastnate.data.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.fastnate.data.DataImportException;
import org.fastnate.data.EntityRegistration;
import org.fastnate.data.csv.properties.DataRow;
import org.fastnate.data.files.DataFile;
import org.fastnate.data.properties.PluralPropertyContents;
import org.fastnate.data.properties.PropertyConverter;
import org.fastnate.data.properties.PropertyDataImporter;
import org.fastnate.generator.context.EmbeddedProperty;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.EntityProperty;
import org.fastnate.generator.context.GeneratedIdProperty;
import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.MapProperty;
import org.fastnate.generator.context.PluralProperty;
import org.fastnate.generator.context.Property;
import org.fastnate.generator.context.SingularProperty;
import org.supercsv.comment.CommentMatches;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * An importer that reads entities of a specific class from a CSV file.
 *
 * One can either {@link #addColumnMapping(String, BiConsumer) add his own column mapping} or use the
 * {@link #mapProperties() default properties mapping}.
 *
 * Column names are handled case insensitive.
 *
 * @author Tobias Liefke
 *
 * @param <E>
 *            The type of the generated objects
 */
@Getter
@Setter
public class CsvDataImporter<E> extends PropertyDataImporter {

	private static final class CsvDataRow extends DataRow {

		@Getter
		@Setter
		private List<String> row;

		/**
		 * Creates a new instance of {@link CsvDataRow}.
		 *
		 * @param header
		 *            the contents of the header row
		 */
		CsvDataRow(final String... header) {
			super(Arrays.asList(header));
		}

		@Override
		public String getValue(final int columnIndex) {
			if (columnIndex >= this.row.size()) {
				return "";
			}
			return this.row.get(columnIndex);
		}

	}

	/**
	 * The name of the property in the {@link GeneratorContext#getSettings() settings} that contains the column
	 * delimiter.
	 *
	 * The default value for this property is '{@code ,}'.
	 */
	public static final String COLUMN_DELIMITER = "fastnate.csv.delimiter.column";

	/**
	 * The name of the property in the {@link GeneratorContext#getSettings() settings} that contains the line delimiter.
	 *
	 * The default value for this property is '{@code \n}'.
	 */
	public static final String LINE_DELIMITER = "fastnate.csv.delimiter.line";

	/**
	 * The name of the property in the {@link GeneratorContext#getSettings() settings} that contains the delimiter for
	 * elements of {@link Collection}s respective {@link Map}s.
	 *
	 * The default value for this property is '{@code ,}'.
	 */
	public static final String COLLECTION_DELIMITER = "fastnate.csv.delimiter.collection";

	/**
	 * The name of the property in the {@link GeneratorContext#getSettings() settings} that contains the delimiter for
	 * the key and the value for each {@link Map} entry (the entries are delimited by {@link #COLLECTION_DELIMITER}).
	 *
	 * The default value for this property is '{@code :}'.
	 */
	public static final String MAP_DELIMITER = "fastnate.csv.delimiter.map";

	private static <V, T> BiConsumer<T, V> buildInverseMapping(final Property<V, ?> inverseProperty) {
		if (inverseProperty instanceof PluralProperty) {
			return (entity, targetEntity) -> PluralPropertyContents
					.create(targetEntity, (PluralProperty<V, ?, T>) inverseProperty).addElement(entity);
		}
		if (inverseProperty instanceof EntityProperty) {
			return (entity, targetEntity) -> ((EntityProperty<V, T>) inverseProperty).setValue(targetEntity, entity);
		}
		return (entity, targetEntity) -> {
			// NOOP
		};
	}

	private static boolean isNotEmpty(final List<String> values) {
		return values.size() > 1 || values.size() == 1 && StringUtils.isNotBlank(values.get(0));
	}

	private static void lowerCaseHeader(final String[] columns) {
		for (int i = 0; i < columns.length; i++) {
			final String column = columns[i];
			if (column != null) {
				columns[i] = column.toLowerCase();
			}
		}
	}

	private static <T, V> Consumer<V> wrapInverseMapping(final BiConsumer<T, V> inverseMapping, final T entity) {
		return value -> inverseMapping.accept(entity, value);
	}

	/** The description of the type of the created entities. */
	private final EntityClass<E> entityClass;

	private final EntityRegistration entityRegistration;

	/** The settings to use when importing a file. */
	private CsvPreference csvSettings;

	/**
	 * The pattern to split {@link Collection} elements or {@link Map} entries, if {@link #mapProperties()} or
	 * {@link #addDefaultColumnMapping(String)} is used. Defaults to the {@link #COLLECTION_DELIMITER} property from the
	 * {@link GeneratorContext#getSettings() settings}.
	 */
	private String collectionDelimiter;

	/**
	 * The pattern to split key and value of each entry in a {@link Map}, if {@link #mapProperties()} or
	 * {@link #addDefaultColumnMapping(String)} is used. Defaults to the {@link #MAP_DELIMITER} property from the
	 * {@link GeneratorContext#getSettings() settings}.
	 */
	private String mapDelimiter;

	/**
	 * Mapping from the name of a column to the function that converts and sets that property for the entity of the
	 * current row.
	 */
	@Getter(AccessLevel.NONE)
	private final Map<String, BiConsumer<E, String>> columnMapping = new HashMap<>();

	/** Indicates to ignore any column that is not found in the CSV file. */
	private boolean ignoreMissingColumns;

	/** Indicates to ignore any column that can't be mapped to a property. */
	private boolean ignoreUnknownColumns;

	/** The columns that are known, but ignored. Only interesting if {@link #ignoreUnknownColumns} is {@code false}. */
	@Getter(AccessLevel.NONE)
	private final Set<String> ignoredColumns = new HashSet<>();

	/** List of functions that are called after the import of each entity. */
	@Getter(AccessLevel.NONE)
	private final List<BiConsumer<DataRow, E>> postProcessors = new ArrayList<>();

	/**
	 * Creates a new instance of {@link CsvDataImporter} for a specific type.
	 *
	 * Will use its own {@link GeneratorContext context} and {@link EntityRegistration}, so use only as standalone
	 * importer.
	 *
	 * @param entityClass
	 *            the type of the created entities
	 */
	public CsvDataImporter(final Class<E> entityClass) {
		this(new GeneratorContext().getDescription(entityClass));
	}

	/**
	 * Creates a new instance of {@link CsvDataImporter} for a specific type.
	 *
	 * Will use its own {@link EntityRegistration}, so use only, if you don't need to exchange entity references between
	 * different importers.
	 *
	 * Will use the default {@link CsvPreference CSV settings}, by taking the properties from the context of the entity
	 * class into account.
	 *
	 * @param entityClass
	 *            the description of the type of the created entities.
	 */
	public CsvDataImporter(final EntityClass<E> entityClass) {
		this(entityClass, new EntityRegistration(entityClass.getContext()));
	}

	/**
	 * Creates a new instance of {@link CsvDataImporter}.
	 *
	 * Will use its own {@link EntityRegistration}, so use only, if you don't need to exchange entity references between
	 * different importers.
	 *
	 * @param entityClass
	 *            the description of the type of the created entities.
	 * @param csvSettings
	 *            the settings to use
	 */
	public CsvDataImporter(final EntityClass<E> entityClass, final CsvPreference csvSettings) {
		this(entityClass, csvSettings, new EntityRegistration(entityClass.getContext()));
	}

	/**
	 * Creates a new instance of {@link CsvDataImporter}.
	 *
	 * @param entityClass
	 *            the description of the type of the created entities.
	 * @param csvSettings
	 *            the settings to use
	 * @param entityRegistration
	 *            used to store unique keys for entities
	 */
	public CsvDataImporter(final EntityClass<E> entityClass, final CsvPreference csvSettings,
			final EntityRegistration entityRegistration) {
		this.entityClass = entityClass;
		this.csvSettings = csvSettings;
		this.entityRegistration = entityRegistration;
		this.collectionDelimiter = entityClass.getContext().getSettings().getProperty(COLLECTION_DELIMITER, ",");
		this.mapDelimiter = entityClass.getContext().getSettings().getProperty(MAP_DELIMITER, ":");
	}

	/**
	 * Creates a new instance of {@link CsvDataImporter} for a specific type.
	 *
	 * @param entityClass
	 *            the description of the type of the created entities.
	 * @param entityRegistration
	 *            used to store unique keys for entities
	 */
	public CsvDataImporter(final EntityClass<E> entityClass, final EntityRegistration entityRegistration) {
		this(entityClass,
				new CsvPreference.Builder('"',
						entityClass.getContext().getSettings().getProperty(COLUMN_DELIMITER, ",").charAt(0),
						entityClass.getContext().getSettings().getProperty(LINE_DELIMITER, "\n"))
								.skipComments(new CommentMatches("(//|#).*")).build(),
				entityRegistration);
	}

	/**
	 * Defines the mapping from a column name to a property.
	 *
	 * @param column
	 *            the name of the column
	 * @param propertyMapping
	 *            converts and sets the value for this column
	 */
	public void addColumnMapping(final String column, final BiConsumer<E, String> propertyMapping) {
		this.columnMapping.put(column.toLowerCase(), propertyMapping);
	}

	/**
	 * Defines the mapping from a column name to a property with an intermediate converter.
	 *
	 * @param <T>
	 *            the type of the property
	 *
	 * @param column
	 *            the name of the column
	 * @param propertyClass
	 *            the type of the property
	 * @param converter
	 *            converts the content of the column to the property
	 * @param propertyMapping
	 *            sets the value for this column
	 */
	public <T> void addColumnMapping(final String column, final Class<T> propertyClass,
			final PropertyConverter<T> converter, final BiConsumer<E, T> propertyMapping) {
		this.columnMapping.put(column.toLowerCase(), (entity, columnValue) -> {
			propertyMapping.accept(entity, converter.convert(propertyClass, columnValue));
		});
	}

	/**
	 * Defines the mapping from a column name to a property with an intermediate converter.
	 *
	 * @param <T>
	 *            the type of the property
	 *
	 * @param column
	 *            the name of the column
	 * @param converter
	 *            converts the content of the column to the property
	 * @param propertyMapping
	 *            sets the value for this column
	 */
	public <T> void addColumnMapping(final String column, final Function<String, T> converter,
			final BiConsumer<E, T> propertyMapping) {
		this.columnMapping.put(column.toLowerCase(), (entity, columnValue) -> {
			propertyMapping.accept(entity, converter.apply(columnValue));
		});
	}

	/**
	 * Defines the mapping from a column name to a property by using the one from {@link #buildMapping(Property)}.
	 *
	 * @param columnName
	 *            the name of the column and property
	 */
	public void addDefaultColumnMapping(final String columnName) {
		addDefaultColumnMapping(columnName, columnName);
	}

	/**
	 * Defines the mapping from a column name to a property by using the one from {@link #buildMapping(Property)}.
	 *
	 * @param columnName
	 *            the name of the column and property
	 * @param propertyName
	 *            the name of the property
	 */
	public void addDefaultColumnMapping(final String columnName, final String propertyName) {
		final Property<? super E, ?> property = this.entityClass.getProperties().get(propertyName);
		if (property == null) {
			throw new IllegalArgumentException("Can't find property \"" + propertyName + '"');
		}
		final BiConsumer<E, String> mapping = buildMapping(property);
		if (mapping == null) {
			throw new IllegalArgumentException("Can't handle property \"" + propertyName + '"');
		}
		this.columnMapping.put(columnName.toLowerCase(), mapping);
	}

	/**
	 * Ignores a column during import.
	 *
	 * Ignored columns only need to be specified if {@link #isIgnoreUnknownColumns()} is set to {@code false}.
	 *
	 * @param column
	 *            the name of the column that is ignored
	 */
	public void addIgnoredColumn(final String column) {
		this.ignoredColumns.add(column.toLowerCase());
	}

	/**
	 * Adds a consumer that is called for each new entity.
	 *
	 * @param postProcessor
	 *            the function to call for each entity
	 */
	public void addPostProcessor(final Consumer<E> postProcessor) {
		this.postProcessors.add((row, entity) -> postProcessor.accept(entity));
	}

	/**
	 * Sets a property for an entity from a column value.
	 *
	 * @param entity
	 *            the entity to modify
	 * @param column
	 *            the name of the current column (in lower case, as the mapping is stored in lower case, too)
	 * @param value
	 *            the value of the column
	 * @return {@code true} if an appropriate property was found, {@code false} if a matching property was not found and
	 *         {@code #isIgnoreUnknownColumns()} is {@code true}
	 *
	 * @throws IllegalArgumentException
	 *             if a matching property was not found or it was not convertable
	 */
	@SuppressWarnings("checkstyle:IllegalCatch")
	protected boolean applyColumn(final E entity, final String column, final String value) {
		if (this.ignoredColumns.contains(column)) {
			return false;
		}

		final BiConsumer<E, String> mapper = this.columnMapping.get(column);
		if (mapper != null) {
			try {
				mapper.accept(entity, value);
				return true;
			} catch (final RuntimeException e) {
				if (e instanceof DataImportException) {
					throw e;
				}
				throw new IllegalArgumentException("Could not map column \"" + column + "\": " + e, e);
			}
		}
		if (!this.ignoreUnknownColumns) {
			throw new IllegalArgumentException(
					"Could not find column property for '" + column + "' in " + entity.getClass());
		}
		return false;
	}

	private <T> BiConsumer<E, String> buildEmbeddedMapping(final EmbeddedProperty<? super E, T> embeddedProperty,
			final Property<T, ?> childProperty) {
		final BiConsumer<T, String> mapping = buildMapping(childProperty);
		if (mapping == null) {
			return null;
		}
		return (entity, value) -> mapping.accept(embeddedProperty.getInitializedValue(entity), value);
	}

	private <V, U> BiConsumer<String, Consumer<V>> buildMapping(final Class<V> valueClass,
			final EntityClass<V> valueEntityClass) {
		// Try to use normal value converter
		final PropertyConverter<V> valueConverter = findConverter(valueClass);
		if (valueConverter != null) {
			return (value, consumer) -> consumer.accept(valueConverter.convert(valueClass, value));
		}

		// Maybe we reference an entity
		if (valueEntityClass == null) {
			return null;
		}

		// Find the unique key of the target class
		final List<List<SingularProperty<V, ?>>> singleUniqueProperties = valueEntityClass.getAllUniqueProperties()
				.stream().filter(list -> list.size() == 1).collect(Collectors.toList());
		if (singleUniqueProperties.size() != 1) {
			// We can't reference the entity by a single unique property
			return null;
		}

		final SingularProperty<V, U> uniqueProperty = (SingularProperty<V, U>) singleUniqueProperties.get(0).get(0);
		final EntityClass<U> targetEntityClass = uniqueProperty instanceof EntityProperty
				? ((EntityProperty<? super V, U>) uniqueProperty).getTargetClass()
				: null;
		final BiConsumer<String, Consumer<U>> uniquePropertyMapping = buildMapping(uniqueProperty.getType(),
				targetEntityClass);
		if (uniquePropertyMapping == null) {
			return null;
		}

		return (value, entityConsumer) -> uniquePropertyMapping.accept(value,
				uniqueValue -> this.entityRegistration.invokeOnEntity(valueEntityClass.getEntityClass(),
						uniqueProperty.getName(), uniqueValue, entityConsumer));
	}

	/**
	 * Builds the mapping for the given property.
	 *
	 * @param <V>
	 *            the type of the property
	 * @param property
	 *            the property to map
	 * @return the mapper of the property or {@code null} if no specific mapping can be created and the property should
	 *         be ignored
	 */
	protected <T, V> BiConsumer<T, String> buildMapping(final Property<? super T, V> property) {
		if (property instanceof EntityProperty) {
			final EntityProperty<? super T, V> entityProperty = (EntityProperty<? super T, V>) property;
			final BiConsumer<String, Consumer<V>> mapping = buildMapping(property.getType(),
					entityProperty.getTargetClass());
			final BiConsumer<Object, V> inverseMapping = buildInverseMapping(entityProperty.getInverseProperty());
			return (entity, value) -> {
				if (StringUtils.isNotEmpty(value)) {
					mapping.accept(value, wrapInverseMapping(inverseMapping, entity)
							.andThen(valueObject -> property.setValue(entity, valueObject)));
				}
			};
		}

		final Class<V> type = property.getType();
		final PropertyConverter<V> converter = findConverter(type);
		if (converter == null) {
			if (property instanceof PluralProperty) {
				return buildPluralMapping((PluralProperty<? super T, ?, ?>) property);
			}

			// We can't handle that property -> ignore it
			return null;
		}
		if (property instanceof GeneratedIdProperty) {
			// Don't set the property, but remember its value in the registry, that we can reference it later
			final String[] propertyNames = new String[] { property.getName() };
			return (entity, value) -> this.entityRegistration.registerEntity(entity, propertyNames,
					new Object[] { converter.convert(type, value) });
		}
		return (entity, value) -> property.setValue(entity, converter.convert(type, value));
	}

	private <T, K, V> BiConsumer<T, String> buildPluralMapping(final PluralProperty<? super T, ?, V> property) {
		if (property.getEmbeddedProperties() != null) {
			// We can't import a collection of embedded properties
			return null;
		}

		// Prepare key mapping
		final BiConsumer<String, Consumer<K>> keyMapping;
		if (property instanceof MapProperty) {
			final MapProperty<T, K, V> mapProperty = (MapProperty<T, K, V>) property;
			keyMapping = buildMapping(mapProperty.getKeyClass(), mapProperty.getKeyEntityClass());
			if (keyMapping == null) {
				return null;
			}
		} else {
			keyMapping = null;
		}

		// Prepare value mapping
		final BiConsumer<String, Consumer<V>> valueMapping = buildMapping(property.getValueClass(),
				property.getValueEntityClass());
		if (valueMapping == null) {
			return null;
		}

		// Ensure that both sides of a bidirectional mapping are filled
		final BiConsumer<T, V> inverseMapping = buildInverseMapping(property.getInverseProperty());

		// Our mapping splits the values and leaves the rest to the key and value mapping
		return (entity, values) -> {
			if (StringUtils.isNotEmpty(values)) {
				final PluralPropertyContents<V> collection = PluralPropertyContents.create(entity, property);
				final String[] split = values.split(this.collectionDelimiter);
				for (int index = 0; index < split.length; index++) {
					final String element = split[index].trim();
					final int currentIndex = index;
					if (keyMapping != null) {
						final int delim = element.indexOf(this.mapDelimiter);
						if (delim < 0) {
							throw new DataImportException(
									"Missing " + this.mapDelimiter + " in value of \"" + property.getName() + '"');
						}
						final String key = element.substring(0, delim).trim();
						final String value = element.substring(delim + 1).trim();
						keyMapping.accept(key, keyObject -> valueMapping.accept(value,
								wrapInverseMapping(inverseMapping, entity).andThen(
										valueObject -> collection.setElement(currentIndex, keyObject, valueObject))));
					} else {
						valueMapping.accept(element, wrapInverseMapping(inverseMapping, entity)
								.andThen(valueObject -> collection.setElement(currentIndex, null, valueObject)));
					}
				}
			}
		};
	}

	private void checkForMissingColumns(final DataFile file, final String[] header) {
		final Collection<String> headers = new HashSet<>(Arrays.asList(header));
		for (final String column : this.columnMapping.keySet()) {
			if (!headers.contains(column)) {
				throw new DataImportException("Missing column: " + column, file.getName(), 0);
			}
		}
	}

	/**
	 * Builds one or more entities from the given row.
	 *
	 * @param row
	 *            contains the current row data
	 * @return the list of entities from that row
	 */
	protected List<? extends E> createEntities(final DataRow row) {
		return Collections.singletonList(createEntity(row));
	}

	/**
	 * Creates a new empty entity for the current converter.
	 *
	 * Used by the default implementation of {@link #createEntities(DataRow)}.
	 *
	 * @return the new entity
	 */
	protected E createEntity() {
		return this.entityClass.newInstance();
	}

	/**
	 * Builds one entity from the given row.
	 *
	 * @param row
	 *            contains the current row data
	 * @return the created and filled entity
	 */
	protected E createEntity(final DataRow row) {
		final E entity = createEntity();
		for (int i = 0; i < row.getColumnCount(); i++) {
			applyColumn(entity, row.getName(i), row.getValue(i));
		}
		return entity;
	}

	/**
	 * Defines the default encoding for CSV files, if it can't be determined from the BOM.
	 *
	 * @return the default encoding
	 */
	protected Charset getDefaultEncoding() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * Reads entities from the given file.
	 *
	 * @param file
	 *            the file to import
	 * @return all found entities
	 *
	 * @throws IOException
	 *             if the file was not accessible
	 * @throws DataImportException
	 *             if the file contents was invalid
	 */
	@SuppressWarnings("checkstyle:IllegalCatch")
	public List<E> importFile(final DataFile file) throws IOException, DataImportException {
		try (CsvListReader csvList = openCsvListReader(file)) {
			final String[] header = csvList.getHeader(true);
			if (header != null && header.length > 0) {
				lowerCaseHeader(header);
				if (!this.ignoreMissingColumns) {
					// Check that all known columns are available
					checkForMissingColumns(file, header);
				}

				// Read all rows
				final List<E> entities = new ArrayList<>();
				final CsvDataRow row = new CsvDataRow(header);
				int rowIndex = 1;
				for (List<String> values; (values = csvList.read()) != null; rowIndex++) {
					if (isNotEmpty(values)) {
						row.setRow(values);
						try {
							final List<? extends E> newEntities = createEntities(row);
							for (final E entity : newEntities) {
								entities.add(entity);
								this.entityRegistration.registerEntity(entity);
								for (final BiConsumer<DataRow, E> postProcessor : this.postProcessors) {
									postProcessor.accept(row, entity);
								}
							}
						} catch (final RuntimeException e) {
							if (e instanceof DataImportException) {
								throw e;
							}
							throw new DataImportException(e.getMessage(), file.getName(), rowIndex, e);
						}
					}
				}
				return entities;
			}
			return Collections.emptyList();
		}

	}

	/**
	 * Indicates that the given column is ignored during import.
	 *
	 * @param column
	 *            the name of the column
	 * @return {@code true} if the column is not imported
	 */
	public boolean isIgnoredColumn(final String column) {
		return this.ignoredColumns.contains(column.toLowerCase());
	}

	/**
	 * Maps the name of each property of the {@link #getEntityClass() entity class} to the CSV columns.
	 *
	 * Useful if the CSV file is a property export.
	 */
	public void mapProperties() {
		for (final Property<? super E, ?> property : this.entityClass.getAllProperties()) {
			if (property instanceof EmbeddedProperty) {
				// We assume that each property of the embedded object has its own column
				final EmbeddedProperty<? super E, Object> embeddedProperty = (EmbeddedProperty<? super E, Object>) property;
				for (final Property<Object, ?> childProperty : embeddedProperty.getEmbeddedProperties().values()) {
					this.columnMapping.computeIfAbsent(
							embeddedProperty.getName().toLowerCase() + '.' + property.getName().toLowerCase(),
							propertyName -> buildEmbeddedMapping(embeddedProperty, childProperty));
				}
			} else if (!(property instanceof GeneratedIdProperty)) {
				this.columnMapping.computeIfAbsent(property.getName().toLowerCase(),
						propertyName -> buildMapping(property));
			}
		}
	}

	/**
	 * Maps the database columns of all properties that are part of the entity table to CSV columns.
	 *
	 * Useful if the CSV file is a database export.
	 */
	public void mapTableColumns() {
		for (final Property<? super E, ?> property : this.entityClass.getAllProperties()) {
			if (property instanceof SingularProperty) {
				final SingularProperty<? super E, ?> singularProperty = (SingularProperty<? super E, ?>) property;
				if (singularProperty.isTableColumn()) {
					final GeneratorColumn column = singularProperty.getColumn();
					if (column != null) {
						this.columnMapping.computeIfAbsent(column.getName().toLowerCase(),
								columnName -> buildMapping(singularProperty));
					}
				}
			}
		}
	}

	/**
	 * Opens a CSV file.
	 *
	 * If the given file ends with "gz", then the file is decompressed using a {@link GZIPInputStream}.
	 *
	 * @param importFile
	 *            the CSV file
	 * @return the reader used to read the CSV file
	 * @throws IOException
	 *             if the file was not accessible
	 */
	protected CsvListReader openCsvListReader(final DataFile importFile) throws IOException {
		// Open file
		InputStream fileStream = importFile.open();

		// Use the reader as marker
		// to distinguish if we have successfully opened the file or if we need to close the stream due to an error
		CsvListReader reader = null;
		try {
			// Check for compressed file
			if (importFile.getName().toLowerCase().endsWith(".gz")) {
				fileStream = new GZIPInputStream(fileStream);
			}

			// Guess the encoding
			final BOMInputStream inputStream = new BOMInputStream(fileStream, false, ByteOrderMark.UTF_8,
					ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
			final String charset;
			if (inputStream.hasBOM()) {
				charset = inputStream.getBOMCharsetName();
			} else {
				charset = getDefaultEncoding().toString();
			}

			return reader = new CsvListReader(new InputStreamReader(inputStream, charset), this.csvSettings);
		} finally {
			if (reader == null) {
				fileStream.close();
			}
		}
	}

	/**
	 * Removes the mapping of the given column.
	 *
	 * @param column
	 *            the column to remove
	 * @return the previous mapping or {@code null} if none was registered
	 */
	public BiConsumer<E, String> removeColumnMapping(final String column) {
		return this.columnMapping.remove(column.toLowerCase());
	}

	/**
	 * Removes an ignored column.
	 *
	 * @param column
	 *            the name of the column that is ignored
	 *
	 * @see #addIgnoredColumn(String)
	 */
	public void removeIgnoredColumn(final String column) {
		this.ignoredColumns.remove(column.toLowerCase());
	}

}
