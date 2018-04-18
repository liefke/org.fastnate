package org.fastnate.generator.context;

import java.util.HashMap;
import java.util.Map;

import org.fastnate.generator.dialect.GeneratorDialect;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

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
	 */
	public AnyMapping(final GeneratorContext context, final AttributeAccessor attribute, final GeneratorColumn column) {
		this.column = column;
		fillMetaDefs(attribute, context.getDialect());
	}

	private void fillMetaDefs(final AttributeAccessor attribute, final GeneratorDialect dialect) {
		final AnyMetaDef metaDef = attribute.getAnnotation(AnyMetaDef.class);
		ModelException.mustExist(metaDef, "Missing AnyMetaDef for {}", attribute);

		for (final MetaValue metaValue : metaDef.metaValues()) {
			this.anyClasses.put(metaValue.targetEntity(), PrimitiveColumnExpression.create(metaValue.value(), dialect));
		}
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
