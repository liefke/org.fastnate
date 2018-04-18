package org.fastnate.generator.context;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.fastnate.generator.converter.BooleanConverter;
import org.fastnate.generator.converter.NumberConverter;
import org.fastnate.generator.converter.StringConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.AnyMetaDefs;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

import com.google.common.base.Supplier;

/**
 * A column that is responsible to store the information for references to arbitrary entity types that are stored in the
 * same attribute.
 *
 * @see Any
 * @see ManyToAny
 *
 * @author Tobias Liefke
 * @param <T>
 *            the type of the attribute that this mapping is attached to
 */
public class AnyMapping<T> {

	private static final Map<String, Supplier<? extends ValueConverter<?>>> META_TYPES = new HashMap<>();

	static {
		// Only a small set of possible meta types (currently we have no "full" mapping of Hibernate types)
		META_TYPES.put("boolean", BooleanConverter::new);
		META_TYPES.put("byte", () -> new NumberConverter(Byte.class));
		META_TYPES.put("short", () -> new NumberConverter(Short.class));
		META_TYPES.put("integer", () -> new NumberConverter(Integer.class));
		META_TYPES.put("long", () -> new NumberConverter(Long.class));
		META_TYPES.put("float", () -> new NumberConverter(Float.class));
		META_TYPES.put("double", () -> new NumberConverter(Double.class));
	}

	private static AnyMetaDef findElementMetaDef(final AnnotatedElement element, final String metaDefName) {
		final AnyMetaDef metaDef = element.getAnnotation(AnyMetaDef.class);
		if (metaDef != null && metaDefName.equals(metaDef.name())) {
			return metaDef;
		}
		final AnyMetaDefs metaDefs = element.getAnnotation(AnyMetaDefs.class);
		if (metaDefs != null) {
			for (final AnyMetaDef metaDefsValue : metaDefs.value()) {
				if (metaDefName.equals(metaDefsValue.name())) {
					return metaDefsValue;
				}
			}
		}
		return null;
	}

	/** The name of the column that contains the id of the entity class, if {@link Any} is used. */
	private final GeneratorColumn column;

	/** Contains the mapping from a class to its id in the database. */
	private final Map<Class<?>, ColumnExpression> anyClasses = new HashMap<>();

	/**
	 * Creates a new instance of {@link AnyMapping}.
	 *
	 * @param context
	 *            the current generation context
	 * @param attribute
	 *            the attribute that this mapping is attached to
	 * @param column
	 *            contains the meta information about the generated column
	 * @param metaDefName
	 *            the name of the global meta definition, if any is used (otherwise the one from the attribute is used)
	 */
	public AnyMapping(final GeneratorContext context, final AttributeAccessor attribute, final GeneratorColumn column,
			final String metaDefName) {
		this.column = column;
		fillMetaDefs(attribute, metaDefName, context);
	}

	private void fillMetaDefs(final AttributeAccessor attribute, final String metaDefName,
			final GeneratorContext context) {
		final AnyMetaDef metaDef;
		if (metaDefName != null) {
			// Global defined AnyMetaDef
			metaDef = findGlobalMetaDef(attribute, metaDefName);
			ModelException.mustExist(metaDef, "Can't find AnyMetaDef with name {} for {} in the class hierarchy",
					metaDefName, attribute);
		} else {
			// Locally defined AnyMetaDef
			metaDef = attribute.getAnnotation(AnyMetaDef.class);
			ModelException.mustExist(metaDef, "Missing AnyMetaDef annotation for {}", attribute);
		}

		final ValueConverter<?> converter = META_TYPES.getOrDefault(metaDef.metaType(), StringConverter::new).get();
		for (final MetaValue metaValue : metaDef.metaValues()) {
			this.anyClasses.put(metaValue.targetEntity(), converter.getExpression(metaValue.value(), context));
		}
	}

	private ColumnExpression findDesc(final T entity) {
		final ColumnExpression desc = this.anyClasses.get(entity.getClass());
		ModelException.mustExist(desc, "Missing AnyMetaDef for {}", entity.getClass());
		return desc;
	}

	private AnyMetaDef findGlobalMetaDef(final AttributeAccessor attribute, final String metaDefName) {
		// Check if the AnyMetaDef is still defined at the attribute (in that case the name is obsolete but doesn't hurt)
		AnyMetaDef metaDef = findElementMetaDef(attribute, metaDefName);
		if (metaDef != null) {
			return metaDef;
		}

		// Now try to find it in the hierarchy of the declaring class ...
		metaDef = findMetaDefInClassHierarchy(attribute.getDeclaringClass(), metaDefName);
		if (metaDef != null) {
			return metaDef;
		}

		// ... or in the hierarchy of the target class
		return findMetaDefInClassHierarchy(attribute.getType(), metaDefName);
	}

	private AnyMetaDef findMetaDefInClassHierarchy(final Class<?> entityClass, final String metaDefName) {
		// Stop recursion, if we reach the top
		if (entityClass == null || entityClass == Object.class) {
			return null;
		}

		// Try to find it in the class declaration ...
		AnyMetaDef metaDef = findElementMetaDef(entityClass, metaDefName);
		if (metaDef != null) {
			return metaDef;
		}

		// ... or in the package declaration
		metaDef = findElementMetaDef(entityClass.getPackage(), metaDefName);
		if (metaDef != null) {
			return metaDef;
		}

		// ... or in one of the interfaces
		for (final Class<?> interf : entityClass.getInterfaces()) {
			metaDef = findMetaDefInClassHierarchy(interf, metaDefName);
			if (metaDef != null) {
				return metaDef;
			}
		}

		// ... or in the super class
		metaDef = findMetaDefInClassHierarchy(entityClass.getSuperclass(), metaDefName);
		if (metaDef != null) {
			return metaDef;
		}

		// .. or in one of the attributes
		return Stream.concat(Stream.of(entityClass.getDeclaredFields()), Stream.of(entityClass.getDeclaredMethods()))
				.map(element -> findElementMetaDef(element, metaDefName)).filter(Objects::nonNull).findFirst()
				.orElse(null);
	}

	/**
	 * Builds a predicate to find the row with the given value.
	 *
	 * Does only work with an additional predicate for the id.
	 *
	 * @param value
	 *            the current value to find as row
	 * @return the predicate to find all rows with the same type of value
	 */
	public String getPredicate(final T value) {
		if (value == null) {
			return this.column.getName() + "IS NULL";
		}
		return this.column.getName() + " = " + findDesc(value);
	}

	/**
	 * Sets the mapping value for the given value to the statement.
	 *
	 * @param statement
	 *            the statement of the current row
	 * @param value
	 *            the value of the current row
	 */
	public void setColumnValue(final TableStatement statement, final T value) {
		if (value == null) {
			statement.setColumnValue(this.column, PrimitiveColumnExpression.NULL);
		}
		statement.setColumnValue(this.column, findDesc(value));
	}

}
