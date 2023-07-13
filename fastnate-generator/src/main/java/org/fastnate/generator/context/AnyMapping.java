package org.fastnate.generator.context;

import jakarta.persistence.Column;
import lombok.Getter;
import org.fastnate.generator.converter.BooleanConverter;
import org.fastnate.generator.converter.NumberConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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

	@Getter
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
	 * @param containerTabble
	 *            the table that contains the type column
	 * @param typeColumn
	 *            contains the meta information about the generated type column
	 * @param metaDefName
	 *            the name of the global meta definition, if any is used (otherwise the one from the attribute is used)
	 */
	public AnyMapping(final GeneratorContext context, final AttributeAccessor attribute,
			final GeneratorTable containerTabble, final Column typeColumn, final String metaDefName) {
		this.column = containerTabble.resolveColumn(typeColumn.name());
	}

	private ColumnExpression findDesc(final T entity) {
		final ColumnExpression desc = this.anyClasses.get(entity.getClass());
		ModelException.mustExist(desc, "Missing AnyMetaDef for {}", entity.getClass());
		return desc;
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
			return this.column.getQualifiedName() + " IS NULL";
		}
		return this.column.getQualifiedName() + " = " + findDesc(value);
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
