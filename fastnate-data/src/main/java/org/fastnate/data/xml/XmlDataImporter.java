package org.fastnate.data.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.fastnate.data.DataImportException;
import org.fastnate.data.EntityRegistration;
import org.fastnate.data.files.DataFile;
import org.fastnate.data.properties.PluralPropertyContents;
import org.fastnate.data.properties.PropertyConverter;
import org.fastnate.data.properties.PropertyDataImporter;
import org.fastnate.generator.context.EmbeddedProperty;
import org.fastnate.generator.context.EntityClass;
import org.fastnate.generator.context.EntityProperty;
import org.fastnate.generator.context.GeneratedIdProperty;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.MapProperty;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.context.PluralProperty;
import org.fastnate.generator.context.PrimitiveProperty;
import org.fastnate.generator.context.Property;
import org.fastnate.generator.context.SingularProperty;

import lombok.RequiredArgsConstructor;

/**
 * Imports entities from a set of XML files in the following format:
 *
 * <pre>
 *   &lt;ArbitraryRoot&gt;
 *     &lt;EntityName primitivePropertyName="... value ..."&gt;
 *       &lt;anotherPrimitivePropertyName&gt;... value ...&lt;/anotherPrimitivePropertyName&gt;
 *       &lt;entityProperty&gt;
 *         &lt;ReferencedEntityName&gt;
 *           ... properties ...
 *         &lt;/ReferencedEntityName&gt;
 *       &lt;/entityProperty&gt;
 *       &lt;anotherEntityProperty&gt;
 *         &lt;ReferencedEntityName reference="true"&gt;
 *           ... unique properties necessary to identify the entity in the {@link EntityRegistration} ...
 *         &lt;/ReferencedEntityName&gt;
 *       &lt;/anotherEntityProperty&gt;
 *       &lt;embeddedProperty&gt;
 *           ... properties ...
 *       &lt;/embeddedProperty&gt;
 *     &lt;/EntityName&gt;
 *     &lt;EntityName&gt;...&lt;/EntityName&gt;
 *     ...
 *   &lt;/ArbitraryRoot&gt;
 * </pre>
 *
 * @author Tobias Liefke
 */
@RequiredArgsConstructor
public class XmlDataImporter extends PropertyDataImporter {

	/** Name of the attribute that indicates that an entity property is only the reference to an existing entity. */
	private static final QName REFERENCE_ATTRIBUTE = new QName("reference");

	/** Name of the attribute that contains the key for a map entry (the value is always the content of the element). */
	private static final QName KEY_ATTRIBUTE = new QName("key");

	private static final Consumer<Object> NOOP_CONSUMER = x -> {
		/* Noop */
	};

	private static void check(final boolean condition, final XMLEvent event, final String errorMessage,
			final Object... parameters) throws XMLStreamException {
		if (!condition) {
			throw new XMLStreamException(ModelException.buildErrorMessage(errorMessage, parameters),
					event == null ? null : event.getLocation());
		}
	}

	private static void checkEndElement(final XMLEvent element, final String expectedName) throws XMLStreamException {
		check(element.isEndElement(), element, "Exptected end of \"{}\", found \"{}\"", expectedName, element);
		final String elementName = element.asEndElement().getName().getLocalPart();
		check(expectedName.equals(elementName), element, "Expected end of \"{}\" instead of \"{}\"", expectedName,
				elementName);
	}

	private static <T> T checkExists(final T value, final XMLEvent event, final String errorMessage,
			final Object... parameters) throws XMLStreamException {
		check(value != null, event, errorMessage, parameters);
		return value;
	}

	private static boolean isEntityReference(final StartElement element) {
		final Attribute reference = element.getAttributeByName(REFERENCE_ATTRIBUTE);
		return reference != null && "true".equals(reference.getValue());
	}

	private static XMLEvent nextEvent(final XMLEventReader reader) throws XMLStreamException {
		try {
			XMLEvent nextEvent = reader.nextEvent();
			while (nextEvent.isCharacters() && nextEvent.asCharacters().isWhiteSpace()) {
				nextEvent = reader.nextEvent();
			}
			return nextEvent;
		} catch (final NoSuchElementException e) {
			throw new XMLStreamException("Unexpected end of document", e);
		}
	}

	private static boolean nextEventIsStartElement(final XMLEventReader reader) throws XMLStreamException {
		return skipWhitespaces(reader).isStartElement();
	}

	private static String readCharacters(final XMLEventReader reader, final String endElement)
			throws XMLStreamException {
		final StringBuilder result = new StringBuilder();
		XMLEvent event = reader.nextEvent();
		while (event != null) {
			switch (event.getEventType()) {
				case XMLStreamConstants.COMMENT:
					// Just skip comments
					break;
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
					if (!event.asCharacters().isIgnorableWhiteSpace()) {
						result.append(event.asCharacters().getData());
					}
					break;
				default:
					checkEndElement(event, endElement);
					return result.toString().trim();
			}
			event = reader.nextEvent();
		}
		throw new XMLStreamException("Unexpected end of document");
	}

	private static XMLEvent skipWhitespaces(final XMLEventReader reader) throws XMLStreamException {
		XMLEvent nextEvent = reader.peek();
		while (nextEvent != null) {
			switch (nextEvent.getEventType()) {
				case XMLStreamConstants.COMMENT:
					// Just skip comments
					break;
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
					if (nextEvent.asCharacters().isWhiteSpace()) {
						break;
					}
					// Fall through - everything else ends the "skip"
				default:
					return nextEvent;
			}
			reader.nextEvent();
			nextEvent = reader.peek();
		}
		return nextEvent;
	}

	private static DataImportException wrapRuntimeException(final RuntimeException e, final DataFile file,
			final XMLEvent nextEvent) {
		return new DataImportException(e.getMessage(), file.getName(),
				nextEvent == null || nextEvent.getLocation() == null ? -1 : nextEvent.getLocation().getLineNumber(),
				nextEvent == null || nextEvent.getLocation() == null ? -1 : nextEvent.getLocation().getColumnNumber(),
				e);
	}

	private static DataImportException wrapStreamException(final XMLStreamException e, final DataFile file) {
		final Location location = e.getLocation();
		return new DataImportException(e.toString(), file.getName(), location == null ? -1 : location.getLineNumber(),
				location == null ? -1
						: location.getColumnNumber() < 0 ? location.getCharacterOffset() : location.getColumnNumber(),
				e);
	}

	/** The generation context. */
	private final GeneratorContext context;

	/** Contains all entities, that have a unique property. */
	private final EntityRegistration entityRegistration;

	/**
	 * Creates a new instance of {@link XmlDataImporter}.
	 *
	 * Uses its own {@link GeneratorContext} and {@link EntityRegistration}, so only use as standalone importer.
	 */
	public XmlDataImporter() {
		this.context = new GeneratorContext();
		this.entityRegistration = new EntityRegistration(this.context);
	}

	private <T> T convert(final XMLEvent element, final String propertyName, final String content, final Class<T> type)
			throws XMLStreamException {
		final PropertyConverter<T> converter = findConverter(type);
		checkExists(converter, element, "Could not find converter for \"{}\"", propertyName);
		return converter.convert(type, content);
	}

	/**
	 * Imports an attribute from XML.
	 *
	 * @param <E>
	 *            the type of the imported entity
	 * @param reader
	 *            the XML stream
	 * @param element
	 *            the element of the entity
	 * @param entity
	 *            the imported entity
	 * @param properties
	 *            all properties known for the entity type
	 * @param attribute
	 *            the imported attribute
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E> void importAttribute(final XMLEventReader reader, final StartElement element, final E entity,
			final Map<String, ? extends Property<? super E, ?>> properties, final Attribute attribute)
			throws XMLStreamException {
		final String elementName = element.getName().getLocalPart();
		final String propertyName = attribute.getName().getLocalPart();
		final Property<? super E, ?> property = properties.get(propertyName);
		checkExists(property, attribute, "Unknown property for attribute \"{}\" of \"{}\"", propertyName, elementName);
		if (property instanceof PrimitiveProperty) {
			final Object value = convert(attribute, property.getName(), attribute.getValue(), property.getType());
			importPrimitiveProperty(reader, entity, (PrimitiveProperty<E, Object>) property, value);
		} else if (property instanceof EntityProperty) {
			// Try to find the entity by its one and only unique ID
			final EntityProperty<E, Object> entityProperty = (EntityProperty<E, Object>) property;
			final List<List<SingularProperty<Object, ?>>> uniqueProperties = entityProperty.getTargetClass()
					.getAllUniqueProperties();
			check(uniqueProperties.size() == 1 && uniqueProperties.get(0).size() == 1, attribute,
					"\"{}\" needs to have exactly one unique property to be referenced as attribute of \"{}\"",
					property, elementName);

			final SingularProperty<Object, ?> uniqueProperty = uniqueProperties.get(0).get(0);
			final Object uniqueValue = convert(attribute, property.getName(), attribute.getValue(),
					uniqueProperty.getType());

			this.entityRegistration.invokeOnEntity(entityProperty.getTargetClass().getEntityClass(),
					uniqueProperty.getName(), uniqueValue,
					targetEntity -> entityProperty.setValue(entity, targetEntity));
		} else {
			throw new XMLStreamException("The property \"" + property.getName()
					+ "\" is not singular and can't be used as attribute of \"" + elementName + '"',
					attribute.getLocation());
		}
	}

	/**
	 * Imports an element from XML for an entity.
	 *
	 * @param <E>
	 *            the type of the imported entity
	 * @param reader
	 *            the XML stream
	 * @param entityElement
	 *            the element of the entity
	 * @param entity
	 *            the imported entity
	 * @param properties
	 *            all properties known for the entity type
	 * @param propertyElement
	 *            the imported element
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E> void importElement(final XMLEventReader reader, final StartElement entityElement, final E entity,
			final Map<String, ? extends Property<? super E, ?>> properties, final StartElement propertyElement)
			throws XMLStreamException {
		final String propertyName = propertyElement.getName().getLocalPart();
		final Property<? super E, ?> property = properties.get(propertyName);
		checkExists(property, propertyElement, "Unknown property for child element \"{}\" of \"{}\"", propertyName,
				entityElement.getName().getLocalPart());
		importProperty(reader, propertyElement, property, entity);
	}

	/**
	 * Imports a property which contains an {@link Embeddable embedded object} from XML.
	 *
	 * @param <E>
	 *            the type of the current entity
	 * @param <T>
	 *            the type of the embedded object
	 * @param reader
	 *            the XML stream
	 * @param propertyElement
	 *            the current element that started the property
	 * @param entity
	 *            the imported entity
	 * @param property
	 *            the imported property of the entity
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E, T> void importEmbeddedProperty(final XMLEventReader reader, final StartElement propertyElement,
			final E entity, final EmbeddedProperty<? super E, T> property) throws XMLStreamException {
		importProperties(reader, propertyElement, property.getEmbeddedProperties(),
				property.getInitializedValue(entity));

		final XMLEvent endElement = reader.nextEvent();
		checkEndElement(endElement, property.getName());
	}

	/**
	 * Imports an entity and invokes a function as soon the entity is imported.
	 *
	 * If the entity is only a {@code reference}, the given handler is invoked as soon the entity is
	 * {@link EntityRegistration#registerEntity(Object) registered}.
	 *
	 * @param <E>
	 *            the type of the imported entity
	 * @param reader
	 *            the XML stream
	 * @param element
	 *            the element that started the import
	 * @param classDescription
	 *            the description of the type of the entity
	 * @param reference
	 *            indicates that the imported entity is just a reference to an existing entity
	 * @param onImport
	 *            the handler to call as soon as the entity is found
	 * @throws XMLStreamException
	 *             if there is an error during import
	 */
	protected <E> void importEntity(final XMLEventReader reader, final StartElement element,
			final EntityClass<E> classDescription, final boolean reference, final Consumer<E> onImport)
			throws XMLStreamException {
		final E entity = classDescription.newInstance();
		importProperties(reader, element, classDescription.getProperties(), entity);

		final XMLEvent endElement = reader.nextEvent();
		checkEndElement(endElement, classDescription.getEntityName());

		if (reference) {
			this.entityRegistration.invokeOnEntity(entity, onImport);
		} else {
			// We found a new entity -> register with its unique property
			this.entityRegistration.registerEntity(entity);

			// And remember the entity
			onImport.accept(entity);
		}
	}

	/**
	 * Imports a property which contains an {@link Entity entity} from XML.
	 *
	 * @param <E>
	 *            the type of the current entity
	 * @param <T>
	 *            the type of the property (the target entity)
	 * @param reader
	 *            the XML stream
	 * @param entity
	 *            the imported entity
	 * @param property
	 *            the imported property of the entity
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E, T> void importEntityProperty(final XMLEventReader reader, final E entity,
			final EntityProperty<E, T> property) throws XMLStreamException {
		final XMLEvent nextEvent = nextEvent(reader);
		check(nextEvent.isStartElement(), nextEvent, "Expecting element with entity name as child of \"{}\"",
				property.getName());
		final StartElement entityElement = nextEvent.asStartElement();
		final String entityName = entityElement.getName().getLocalPart();
		final EntityClass<T> childClassDescription = checkExists(
				(EntityClass<T>) this.context.getDescriptionsByName().get(entityName), entityElement,
				"Unknown entity type: {}", entityName);
		check(property.getTargetClass().getEntityClass().isAssignableFrom(childClassDescription.getEntityClass()),
				entityElement, "Expected at least \"{}\" for \"{}\", found \"{}\"",
				property.getTargetClass().getEntityName(), property.getName(), childClassDescription.getEntityName());

		// Set the value, as soon as we encounter the entity
		Consumer<T> onImport = targetEntity -> property.setValue(entity, targetEntity);

		// Ensure that both sides of a bidirectional mapping are filled
		final Property<T, ?> inverseProperty = property.getInverseProperty();
		if (inverseProperty instanceof PluralProperty) {
			onImport = onImport.andThen(targetEntity -> PluralPropertyContents
					.create(targetEntity, (PluralProperty<T, ?, E>) inverseProperty).addElement(entity));
		} else if (inverseProperty instanceof EntityProperty) {
			onImport = onImport
					.andThen(targetEntity -> ((EntityProperty<T, E>) inverseProperty).setValue(targetEntity, entity));
		}

		importEntity(reader, entityElement, childClassDescription, isEntityReference(entityElement), onImport);

		checkEndElement(nextEvent(reader), property.getName());
	}

	/**
	 * Imports all entites found in the given XML file.
	 *
	 * The root element in the XML is ignored (it's only used as a container).
	 *
	 * @param file
	 *            the file
	 * @return the imported entities
	 * @throws IOException
	 *             if there was a problem when accessing the file
	 * @throws DataImportException
	 *             if the XML or its contents was invalid
	 */
	@SuppressWarnings("IllegalCatch")
	public List<Object> importFile(final DataFile file) throws IOException, DataImportException {
		try (InputStream input = file.open()) {

			final XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(input);
			while (reader.hasNext() && !reader.peek().isStartElement()) {
				reader.next();
			}
			if (!reader.hasNext()) {
				throw new XMLStreamException("Unexpected end of document");
			}

			final StartElement containerElement = nextEvent(reader).asStartElement();

			final List<Object> entities = new ArrayList<>();
			try {
				while (nextEventIsStartElement(reader)) {
					final StartElement element = reader.nextEvent().asStartElement();
					final String entityName = element.getName().getLocalPart();
					final EntityClass<Object> classDescription = checkExists(
							(EntityClass<Object>) this.context.getDescriptionsByName().get(entityName), element,
							"Unsupported element: {}", entityName);
					importEntity(reader, element, classDescription, false, entities::add);
				}
			} catch (final RuntimeException e) {
				final XMLEvent nextEvent = reader.peek();
				throw wrapRuntimeException(e, file, nextEvent);
			}

			checkEndElement(reader.nextEvent(), containerElement.getName().getLocalPart());

			final XMLEvent endElement = nextEvent(reader);
			check(endElement.isEndDocument(), endElement, "Expected end of file, found: {}", endElement);

			reader.close();

			return entities;
		} catch (final FactoryConfigurationError e) {
			throw new IllegalStateException(e);
		} catch (final XMLStreamException e) {
			throw wrapStreamException(e, file);
		}
	}

	/**
	 * Imports a {@link Collection} or {@link Map} property from XML.
	 *
	 * @param <E>
	 *            the type of the current entity
	 * @param <T>
	 *            the type of the embedded object
	 * @param reader
	 *            the XML stream
	 * @param entity
	 *            the imported entity
	 * @param property
	 *            the imported property of the entity
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E, T> void importPluralProperty(final XMLEventReader reader, final E entity,
			final PluralProperty<? super E, ?, T> property) throws XMLStreamException {
		final List<Property<T, ?>> embeddedProperties = property.getEmbeddedProperties();
		final EntityClass<T> targetClass = property.getValueEntityClass();
		final String elementName = targetClass != null ? targetClass.getEntityName()
				: property.getValueClass().getSimpleName();

		// Ensure that both sides of a bidirectional mapping are filled
		final Consumer<T> onImport;
		final Property<T, ?> inverseProperty = property.getInverseProperty();
		if (inverseProperty instanceof PluralProperty) {
			onImport = targetEntity -> PluralPropertyContents
					.create(targetEntity, (PluralProperty<T, ?, E>) inverseProperty).addElement(entity);
		} else if (inverseProperty instanceof EntityProperty) {
			onImport = targetEntity -> ((EntityProperty<T, E>) inverseProperty).setValue(targetEntity, entity);
		} else {
			onImport = (Consumer<T>) NOOP_CONSUMER;
		}

		// Prepare key converter
		final Class<Object> keyType;
		final PropertyConverter<Object> keyConverter;
		if (property instanceof MapProperty) {
			keyType = ((MapProperty<E, Object, T>) property).getKeyClass();
			keyConverter = findConverter(keyType);
			checkExists(keyConverter, reader.peek(), "Could not find converter for key of \"{}\"", property);
		} else {
			keyType = null;
			keyConverter = null;
		}

		// Parse the collection
		final PluralPropertyContents<T> collection = PluralPropertyContents.create(entity, property);
		for (int index = 0; nextEventIsStartElement(reader); index++) {
			final StartElement element = reader.nextEvent().asStartElement();
			final String localPart = element.getName().getLocalPart();
			check(localPart.equals(elementName), element, "Expected \"{}\", found \"{}\"", elementName, localPart);

			final Object key;
			if (keyConverter != null) {
				final Attribute keyAttribute = element.getAttributeByName(KEY_ATTRIBUTE);
				checkExists(keyAttribute, element, "Missing key attribute for \"{}\"", property);
				key = keyConverter.convert(keyType, keyAttribute.getValue());
			} else {
				key = null;
			}

			final int currentIndex = index;
			if (embeddedProperties != null) {
				final T value = property.newElement();
				collection.setElement(index, key, value);
				importProperties(reader, element, property.getEmbeddedPropertiesByName(), value);
				checkEndElement(nextEvent(reader), elementName);
			} else if (targetClass == null) {
				final T value = convert(element, elementName, readCharacters(reader, elementName),
						property.getValueClass());
				collection.setElement(currentIndex, key, value);
				checkEndElement(nextEvent(reader), elementName);
			} else {
				importEntity(reader, element, targetClass, isEntityReference(element),
						onImport.andThen(targetEntity -> collection.setElement(currentIndex, key, targetEntity)));
			}

		}

		checkEndElement(reader.nextEvent(), property.getName());
	}

	/**
	 * Imports the given property from XML.
	 *
	 * The default implementation reads and converts the value and calls
	 * {@link #importPrimitiveProperty(XMLEventReader, Object, PrimitiveProperty, Object)}.
	 *
	 * @param reader
	 *            the XML stream
	 * @param entity
	 *            the imported entity
	 * @param property
	 *            the imported property
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E, T> void importPrimitiveProperty(final XMLEventReader reader, final E entity,
			final PrimitiveProperty<E, T> property) throws XMLStreamException {
		final XMLEvent event = reader.peek();
		final String content = readCharacters(reader, property.getName());
		final T value = convert(event, property.getName(), content, property.getType());
		importPrimitiveProperty(reader, entity, property, value);
	}

	/**
	 * Imports the given value for the given property from XML.
	 *
	 * @param reader
	 *            the XML stream
	 * @param entity
	 *            the imported entity
	 * @param property
	 *            the imported property
	 * @param value
	 *            the imported value
	 */
	protected <E, T> void importPrimitiveProperty(final XMLEventReader reader, final E entity,
			final PrimitiveProperty<E, T> property, final T value) {
		if (!(property instanceof GeneratedIdProperty)) {
			property.setValue(entity, value);
		}
	}

	/**
	 * Imports the given set of properties from the XML file for the given entity.
	 *
	 * @param <E>
	 *            the type of the entity
	 * @param reader
	 *            the XML stream
	 * @param element
	 *            the element of the entity
	 * @param properties
	 *            all properties known for the entity type
	 * @param entity
	 *            the entity to import
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E> void importProperties(final XMLEventReader reader, final StartElement element,
			final Map<String, ? extends Property<? super E, ?>> properties, final E entity) throws XMLStreamException {
		for (final Iterator<?> attributes = element.getAttributes(); attributes.hasNext();) {
			final Attribute attribute = (Attribute) attributes.next();
			if (!attribute.getName().equals(REFERENCE_ATTRIBUTE)) {
				importAttribute(reader, element, entity, properties, attribute);
			}
		}

		while (nextEventIsStartElement(reader)) {
			importElement(reader, element, entity, properties, reader.nextEvent().asStartElement());
		}
	}

	/**
	 * Imports one property from the XML file.
	 *
	 * @param <E>
	 *            the type of the imported entity
	 * @param reader
	 *            the XML stream
	 * @param element
	 *            the element that triggered the import of the property
	 * @param property
	 *            the imported property
	 * @param entity
	 *            the imported entity
	 * @throws XMLStreamException
	 *             if the XML is invalid
	 */
	protected <E, T> void importProperty(final XMLEventReader reader, final StartElement element,
			final Property<? super E, T> property, final E entity) throws XMLStreamException {
		if (property instanceof PluralProperty) {
			importPluralProperty(reader, entity, (PluralProperty<? super E, T, ?>) property);
		} else if (property instanceof EmbeddedProperty) {
			importEmbeddedProperty(reader, element, entity, (EmbeddedProperty<? super E, T>) property);
		} else if (property instanceof EntityProperty) {
			importEntityProperty(reader, entity, (EntityProperty<E, T>) property);
		} else if (property instanceof PrimitiveProperty) {
			importPrimitiveProperty(reader, entity, (PrimitiveProperty<E, Object>) property);
		}
	}

}
