package org.fastnate.generator.context;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.fastnate.generator.EntitySqlGenerator;
import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.dialect.H2Dialect;
import org.fastnate.generator.provider.HibernateProvider;
import org.fastnate.generator.provider.JpaProvider;
import org.fastnate.generator.statements.StatementsWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the configuration and state for one or more {@link EntitySqlGenerator}s.
 *
 * @author Tobias Liefke
 */
@Getter
@Setter
@Slf4j
public class GeneratorContext {

	@RequiredArgsConstructor
	private static final class GeneratorId {

		private final String id;

		private final GeneratorTable table;

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof GeneratorId) {
				final GeneratorId other = (GeneratorId) obj;
				return this.id.equals(other.id) && Objects.equals(this.table, other.table);
			}
			return false;
		}

		@Override
		public int hashCode() {
			if (this.table == null) {
				return this.id.hashCode();
			}
			return this.id.hashCode() << 2 | this.table.getName().hashCode();
		}

		@Override
		public String toString() {
			if (this.table == null) {
				return this.id;
			}
			return this.id + '.' + this.table.getName();
		}

	}

	/**
	 * The settings key for the JPA provider.
	 *
	 * Contains either the fully qualified class name of an extension of {@link JpaProvider} or the simple name of one
	 * of the classes from {@code org.fastnate.generator.provider}.
	 *
	 * Defaults to {@code HibernateProvider}.
	 */
	public static final String PROVIDER_KEY = "fastnate.generator.jpa.provider";

	/** The settings key for the path to the persistence.xml, either relative to the current directory or absolute. */
	public static final String PERSISTENCE_FILE_KEY = "fastnate.generator.persistence.file";

	/**
	 * The settings key for the name of the persistence unit in the persistence.xml. The first persistence unit is used,
	 * if none is explicitly set.
	 */
	public static final String PERSISTENCE_UNIT_KEY = "fastnate.generator.persistence.unit";

	/**
	 * The settings key for the target SQL dialect.
	 *
	 * <p>
	 * Contains either the fully qualified name of a class that extends {@link GeneratorDialect} or the simple class
	 * name of one of the classes from {@code org.fastnate.generator.dialect}. The suffix 'Dialect' may be omitted in
	 * that case. For example 'MySql' would map to {@code org.fastnate.generator.dialect.MySqlDialect}.
	 * </p>
	 *
	 * <p>
	 * If no dialect is set explicitly then the configured {@link #PERSISTENCE_FILE_KEY persistence.xml} is scanned for
	 * a connection URL or provider specific dialect, which would be converted to our known dialects.
	 * </p>
	 *
	 * <p>
	 * If nothing is found, H2 is used as default.
	 * </p>
	 */
	public static final String DIALECT_KEY = "fastnate.generator.dialect";

	/** The settings key for {@link #writeNullValues}. */
	public static final String NULL_VALUES_KEY = "fastnate.generator.null.values";

	/** The settings key for {@link #writeRelativeIds}. */
	public static final String RELATIVE_IDS_KEY = "fastnate.generator.relative.ids";

	/** The settings key for the {@link #uniquePropertyQuality}. */
	public static final String UNIQUE_PROPERTIES_QUALITY_KEY = "fastnate.generator.unique.properties.quality";

	/** The settings key for the {@link #maxUniqueProperties}. */
	public static final String UNIQUE_PROPERTIES_MAX_KEY = "fastnate.generator.unique.properties.max";

	/** The settings key for {@link #preferSequenceCurentValue}. */
	public static final String PREFER_SEQUENCE_CURRENT_VALUE = "fastnate.generator.prefer.sequence.current.value";

	/**
	 * Tries to read any persistence file defined in the settings.
	 *
	 * @param settings
	 *            the current settings
	 */
	private static void readPersistenceFile(final Properties settings) {
		String persistenceFilePath = settings.getProperty(PERSISTENCE_FILE_KEY);
		if (StringUtils.isEmpty(persistenceFilePath)) {
			final URL url = GeneratorContext.class.getResource("/META-INF/persistence.xml");
			if (url == null) {
				return;
			}
			persistenceFilePath = url.toString();
		} else {
			final File persistenceFile = new File(persistenceFilePath);
			if (persistenceFile.isFile()) {
				persistenceFilePath = persistenceFile.toURI().toString();
			}
		}

		final String persistenceUnit = settings.getProperty(PERSISTENCE_UNIT_KEY);
		try {
			final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(persistenceFilePath);
			final NodeList persistenceUnits = document.getElementsByTagName("persistence-unit");
			for (int i = 0; i < persistenceUnits.getLength(); i++) {
				final Element persistenceUnitElement = (Element) persistenceUnits.item(i);
				if (StringUtils.isEmpty(persistenceUnit)
						|| persistenceUnit.equals(persistenceUnitElement.getAttribute("name"))) {
					final NodeList properties = persistenceUnitElement.getElementsByTagName("property");
					for (int i2 = 0; i2 < properties.getLength(); i2++) {
						final Element property = (Element) properties.item(i2);
						final String name = property.getAttribute("name");
						if (!settings.containsKey(name)) {
							settings.put(name, property.getAttribute("value"));
						}
					}
					break;
				}
			}
		} catch (final IOException | SAXException | ParserConfigurationException e) {
			log.error("Could not read " + persistenceFilePath + ": " + e, e);
		}
	}

	/** Identifies the SQL dialect for generating SQL statements. Encapsulates the database specifica. */
	private GeneratorDialect dialect;

	/** Identifies the JPA provider to indicate implementation specific details. */
	private JpaProvider provider;

	/** The maximum count of columns that are used when referencing an entity using it's unique properties. */
	private int maxUniqueProperties = 1;

	/**
	 * Indicates what kind of properties are used for referencing an entity with its unique properties.
	 */
	private UniquePropertyQuality uniquePropertyQuality = UniquePropertyQuality.onlyRequiredPrimitives;

	/**
	 * Indiciates to use "currval" of a sequence if the referenced entity is the last created entity for that sequence
	 * before checking for {@link #uniquePropertyQuality unique properties}.
	 */
	private boolean preferSequenceCurentValue = true;

	/**
	 * Indicates that we write into a schema that is not empty. By default we write all IDs as absolute values and
	 * change the sequences / table generators at the end. But this would crash if there is data in the database already
	 * that uses the same IDs. So in the case of incremental updates, one should set this setting to {@code true} -
	 * which will generate relative IDs which respect the existing IDs.
	 */
	private boolean writeRelativeIds;

	/** Indicates to include null values in statements. */
	private boolean writeNullValues;

	/** Contains the settings that were given during creation, resp. as read from the persistence configuration. */
	private final Properties settings;

	/** Contains the extracted metadata to every known class of an {@link Entity}. */
	private final Map<Class<?>, EntityClass<?>> descriptions = new HashMap<>();

	/** Mapping from the names of all known database table to their description (including column information). */
	private final Map<String, GeneratorTable> tables = new HashMap<>();

	/** Contains the state of single entities, maps from an entity name to the mapping of an id to its state. */
	private final Map<String, Map<Object, GenerationState>> states = new HashMap<>();

	/** Mapping from the name of a generator to the generator itself. */
	@Getter(AccessLevel.NONE)
	private final Map<GeneratorId, IdGenerator> generators = new HashMap<>();

	/** The default sequence generator, if none is explicitly specified in a {@link GeneratedValue}. */
	private SequenceIdGenerator defaultSequenceGenerator;

	/** The default table generator, if none is explicitly specified in a {@link GeneratedValue}. */
	private TableIdGenerator defaultTableGenerator;

	/** All listeners of this context. */
	private List<ContextModelListener> contextModelListeners = new ArrayList<>();

	/**
	 * Creates a default generator context.
	 */
	public GeneratorContext() {
		this(new H2Dialect());
	}

	/**
	 * Creates a generator context for a dialect.
	 *
	 * @param dialect
	 *            the database dialect to use during generation
	 */
	public GeneratorContext(final GeneratorDialect dialect) {
		this.dialect = dialect;
		this.provider = new HibernateProvider();
		this.settings = new Properties();
	}

	/**
	 * Creates a new instance of {@link GeneratorContext}.
	 *
	 * @param settings
	 *            contains the settings
	 */
	public GeneratorContext(final Properties settings) {
		this.settings = settings;

		readPersistenceFile(settings);

		String providerName = settings.getProperty(PROVIDER_KEY, "HibernateProvider");
		if (providerName.indexOf('.') < 0) {
			providerName = JpaProvider.class.getPackage().getName() + '.' + providerName;
		}
		try {
			this.provider = (JpaProvider) Class.forName(providerName).newInstance();
			this.provider.initialize(settings);
		} catch (final InstantiationException | IllegalAccessException | ClassNotFoundException
				| ClassCastException e) {
			throw new IllegalArgumentException("Can't instantiate provider: " + providerName, e);
		}

		String dialectName = settings.getProperty(DIALECT_KEY, "H2Dialect");
		if (dialectName.indexOf('.') < 0) {
			dialectName = GeneratorDialect.class.getPackage().getName() + '.' + dialectName;
			if (!dialectName.endsWith("Dialect")) {
				dialectName += "Dialect";
			}
		}
		try {
			this.dialect = (GeneratorDialect) Class.forName(dialectName).newInstance();
		} catch (final InstantiationException | IllegalAccessException | ClassNotFoundException
				| ClassCastException e) {
			throw new IllegalArgumentException("Can't instantiate dialect: " + dialectName, e);
		}

		this.writeRelativeIds = Boolean
				.parseBoolean(settings.getProperty(RELATIVE_IDS_KEY, String.valueOf(this.writeRelativeIds)));
		this.writeNullValues = Boolean
				.parseBoolean(settings.getProperty(NULL_VALUES_KEY, String.valueOf(this.writeNullValues)));
		this.uniquePropertyQuality = UniquePropertyQuality
				.valueOf(settings.getProperty(UNIQUE_PROPERTIES_QUALITY_KEY, this.uniquePropertyQuality.name()));
		this.maxUniqueProperties = Integer
				.parseInt(settings.getProperty(UNIQUE_PROPERTIES_MAX_KEY, String.valueOf(this.maxUniqueProperties)));
		this.preferSequenceCurentValue = Boolean.parseBoolean(
				settings.getProperty(PREFER_SEQUENCE_CURRENT_VALUE, String.valueOf(this.preferSequenceCurentValue)));
	}

	/**
	 * Adds a new listener to this context.
	 *
	 * @param listener
	 *            the listener that is interested in new discovered model elements
	 */
	public void addContextModelListener(final ContextModelListener listener) {
		this.contextModelListeners.add(listener);
	}

	private <K, T> T addContextObject(final Map<K, ? super T> objects,
			final BiConsumer<ContextModelListener, ? super T> listenerFunction, final K key, final T object) {
		objects.put(key, object);
		fireContextObjectAdded(listenerFunction, object);
		return object;
	}

	/**
	 * Fires an event to all {@link #getContextModelListeners() listeners}.
	 *
	 * @param listenerFunction
	 *            the function that is called on the listeners
	 * @param contextObject
	 *            the object to offer to the listener function
	 */
	protected <T> void fireContextObjectAdded(final BiConsumer<ContextModelListener, T> listenerFunction,
			final T contextObject) {
		for (final ContextModelListener listener : this.contextModelListeners) {
			listenerFunction.accept(listener, contextObject);
		}
	}

	private IdGenerator getDefaultSequenceGenerator() {
		if (this.defaultSequenceGenerator == null) {
			this.defaultSequenceGenerator = new SequenceIdGenerator(
					AnnotationDefaults
							.create(SequenceGenerator.class,
									ImmutableMap.of("sequenceName", this.provider.getDefaultSequence(),
											"allocationSize", Integer.valueOf(1))),
					this.dialect, this.writeRelativeIds);
			fireContextObjectAdded(ContextModelListener::foundGenerator, this.defaultSequenceGenerator);
		}
		return this.defaultSequenceGenerator;
	}

	private IdGenerator getDefaultTableGenerator() {
		if (this.defaultTableGenerator == null) {
			this.defaultTableGenerator = new TableIdGenerator(AnnotationDefaults.create(TableGenerator.class,
					ImmutableMap.of("pkColumnValue", "default", "allocationSize", Integer.valueOf(1))), this);
			fireContextObjectAdded(ContextModelListener::foundGenerator, this.defaultTableGenerator);
		}
		return this.defaultTableGenerator;
	}

	/**
	 * Finds the description for a class.
	 *
	 * @param entityClass
	 *            the class to lookup
	 * @return the description for the class or {@code null} if the class is not an {@link Entity}
	 */
	public <E> EntityClass<E> getDescription(final Class<E> entityClass) {
		// Lookup description
		EntityClass<E> description = (EntityClass<E>) this.descriptions.get(entityClass);
		if (description == null) {
			if (entityClass.isAnnotationPresent(Entity.class)) {
				// Description not build up to now

				// Create the description
				description = new EntityClass<>(this, entityClass);

				// First remember the description (to prevent endless loops)
				this.descriptions.put(entityClass, description);

				// And now build the properties
				description.build();

				// And notify listeners
				fireContextObjectAdded(ContextModelListener::foundEntityClass, description);
			} else {
				// Step up to find the parent description
				final Class<?> superClass = entityClass.getSuperclass();
				if (superClass == null) {
					return null;
				}

				description = (EntityClass<E>) getDescription(superClass);
				if (description != null) {
					// Just remember description for our subclass
					this.descriptions.put(entityClass, description);
				}
			}

		}
		return description;
	}

	/**
	 * Finds the description for the class of an entity.
	 *
	 * @param entity
	 *            the entity to lookup
	 * @return the description for the class of the entity
	 * @throws IllegalArgumentException
	 *             if the given object is no {@link Entity}
	 */
	public <E> EntityClass<E> getDescription(final E entity) {
		if (entity == null) {
			throw new IllegalArgumentException("Can't inspect null entity");
		}
		final EntityClass<E> description = (EntityClass<E>) getDescription(entity.getClass());
		if (description == null) {
			throw new IllegalArgumentException(entity.getClass() + " is not an entity class");
		}
		return description;
	}

	/**
	 * Finds the correct generator for the given annotation.
	 *
	 * @param generatedValue
	 *            the annotation of the current primary key
	 * @param table
	 *            the name of the current table
	 * @param column
	 *            the name of the current column
	 * @return the generator that is responsible for managing the values
	 */
	@SuppressWarnings("null")
	public IdGenerator getGenerator(final GeneratedValue generatedValue, final GeneratorTable table,
			final GeneratorColumn column) {
		GenerationType strategy = generatedValue.strategy();
		final String name = generatedValue.generator();
		if (StringUtils.isNotEmpty(name)) {
			ModelException.test(strategy != GenerationType.IDENTITY,
					"Generator for GenerationType.IDENTITY not allowed");
			IdGenerator generator = this.generators.get(new GeneratorId(name, table));
			if (generator == null) {
				generator = this.generators.get(new GeneratorId(name, null));
				ModelException.test(generator != null, "Generator '{}' not found", name);

				final IdGenerator derived = generator.derive(table);
				if (derived != generator) {
					return addContextObject(this.generators, ContextModelListener::foundGenerator,
							new GeneratorId(name, table), derived);
				}
			}
			return generator;
		}
		if (strategy == GenerationType.AUTO) {
			strategy = this.dialect.getAutoGenerationType();
		}
		switch (strategy) {
			case IDENTITY:
				return addContextObject(this.generators, ContextModelListener::foundGenerator,
						new GeneratorId(column.getName(), table), new IdentityValue(this, table, column));
			case TABLE:
				return getDefaultTableGenerator();
			case SEQUENCE:
				return getDefaultSequenceGenerator();
			case AUTO:
			default:
				throw new ModelException("Unknown GenerationType: " + strategy);
		}
	}

	/**
	 * The entity states for the given entity class.
	 *
	 * @param entityClass
	 *            the current entity class
	 * @return the states of the entities of that class (with their IDs as keys)
	 */
	Map<Object, GenerationState> getStates(final EntityClass<?> entityClass) {
		Map<Object, GenerationState> entityStates = this.states.get(entityClass.getEntityName());
		if (entityStates == null) {
			entityStates = new HashMap<>();
			this.states.put(entityClass.getEntityName(), entityStates);
		}
		return entityStates;
	}

	/**
	 * Registers the {@link TableGenerator} and {@link SequenceGenerator} declared at the given element.
	 *
	 * If neither annotation is present, nothing happens.
	 *
	 * @param element
	 *            the inspected class, method or field
	 * @param table
	 *            the table of the current entity
	 */
	public void registerGenerators(final AnnotatedElement element, final GeneratorTable table) {
		final SequenceGenerator sequenceGenerator = element.getAnnotation(SequenceGenerator.class);
		if (sequenceGenerator != null) {
			GeneratorId key = new GeneratorId(sequenceGenerator.name(), null);
			final IdGenerator existingGenerator = this.generators.get(key);
			if (!(existingGenerator instanceof SequenceIdGenerator) || !((SequenceIdGenerator) existingGenerator)
					.getSequenceName().equals(sequenceGenerator.sequenceName())) {
				if (existingGenerator != null) {
					key = new GeneratorId(sequenceGenerator.name(), table);
				}
				addContextObject(this.generators, ContextModelListener::foundGenerator, key,
						new SequenceIdGenerator(sequenceGenerator, this.dialect, this.writeRelativeIds));
			}
		}

		final TableGenerator tableGenerator = element.getAnnotation(TableGenerator.class);
		if (tableGenerator != null) {
			final GeneratorId key = new GeneratorId(tableGenerator.name(), null);
			if (!this.generators.containsKey(key)) {
				addContextObject(this.generators, ContextModelListener::foundGenerator, key,
						new TableIdGenerator(tableGenerator, this));
			}
		}
	}

	/**
	 * Removes a listener from this context.
	 *
	 * @param listener
	 *            the listener that is not interested anymore
	 */
	public void removeContextModelListener(final ContextModelListener listener) {
		this.contextModelListeners.remove(listener);
	}

	/**
	 * Finds resp. builds the metadata to the given table.
	 *
	 * @param tableName
	 *            the name of the table from the database
	 * @return the metadata for the given table
	 */
	public GeneratorTable resolveTable(final String tableName) {
		final GeneratorTable table = this.tables.get(tableName);
		if (table != null) {
			return table;
		}
		return addContextObject(this.tables, ContextModelListener::foundTable, tableName,
				new GeneratorTable(this.tables.size(), tableName, this));
	}

	/**
	 * Builds all statements that are necessary to align ID generators in the database with the current IDs.
	 *
	 * @param writer
	 *            the target of any write operation
	 * @throws IOException
	 *             if the writer throws one
	 */
	public void writeAlignmentStatements(final StatementsWriter writer) throws IOException {
		for (final IdGenerator generator : this.generators.values()) {
			generator.alignNextValue(writer);
		}
		if (this.defaultSequenceGenerator != null) {
			this.defaultSequenceGenerator.alignNextValue(writer);
		}
		if (this.defaultTableGenerator != null) {
			this.defaultTableGenerator.alignNextValue(writer);
		}
	}
}
