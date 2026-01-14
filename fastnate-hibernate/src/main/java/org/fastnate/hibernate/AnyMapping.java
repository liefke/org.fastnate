package org.fastnate.hibernate;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;

import org.fastnate.generator.context.AttributeAccessor;
import org.fastnate.generator.context.GeneratorColumn;
import org.fastnate.generator.context.GeneratorContext;
import org.fastnate.generator.context.GeneratorTable;
import org.fastnate.generator.context.ModelException;
import org.fastnate.generator.converter.NumberConverter;
import org.fastnate.generator.converter.StringConverter;
import org.fastnate.generator.converter.ValueConverter;
import org.fastnate.generator.statements.ColumnExpression;
import org.fastnate.generator.statements.PrimitiveColumnExpression;
import org.fastnate.generator.statements.TableStatement;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.ManyToAny;

import lombok.Getter;

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

	private static <A extends Annotation> A findAnnotation(final AttributeAccessor attribute,
			final Class<A> annotationClass) {
		A annotation = attribute.getAnnotation(annotationClass);
		if (annotation != null) {
			return annotation;
		}
		for (final Annotation a : attribute.getAnnotations()) {
			annotation = a.annotationType().getAnnotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	/** The column that contains the identifier of the type disciminator. */
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
	 * @param containerTable
	 *            the table that contains the discriminator column
	 * @param attributeOverride
	 *            contains the optional attribute overrides for the discriminator column
	 */
	public AnyMapping(final GeneratorContext context, final AttributeAccessor attribute,
			final GeneratorTable containerTable, final AttributeOverride attributeOverride) {
		final Column discriminatorColumnDefinition = attributeOverride != null
				&& attributeOverride.column().length() > 0 ? attributeOverride.column()
						: attribute.getAnnotation(Column.class);
		if (discriminatorColumnDefinition != null && discriminatorColumnDefinition.name().length() > 0) {
			this.column = containerTable.resolveColumn(discriminatorColumnDefinition.name());
		} else {
			this.column = containerTable.resolveColumn(attribute.getName());
		}

		final AnyDiscriminatorValues values = findAnnotation(attribute, AnyDiscriminatorValues.class);
		ModelException.mustExist(values, "Can't find @AnyDiscriminatorValues for {}", attribute);

		final ValueConverter<?> converter;
		final AnyDiscriminator anyDiscriminator = findAnnotation(attribute, AnyDiscriminator.class);
		if (anyDiscriminator != null && anyDiscriminator.value() == DiscriminatorType.INTEGER) {
			converter = new NumberConverter(Integer.class);
		} else {
			converter = new StringConverter(discriminatorColumnDefinition, false);
		}

		final Set<String> duplicates = new HashSet<>();
		for (final AnyDiscriminatorValue discriminatorValue : values.value()) {
			final Class<?> entity = discriminatorValue.entity();
			final String discriminator = discriminatorValue.discriminator();
			ModelException.test(this.anyClasses.put(entity, converter.getExpression(discriminator, context)) == null,
					"The entity type {} is defined twice for AnyDiscriminatorValues assigned to {}", entity, attribute);
			ModelException.test(duplicates.add(discriminator),
					"The discriminator {} is defined twice for AnyDiscriminatorValues assigned to {}", discriminator,
					attribute);
		}
	}

	private ColumnExpression findDesc(final T entity) {
		final Class<? extends Object> entityClass = entity.getClass();
		final ColumnExpression desc = this.anyClasses.get(entityClass);
		ModelException.mustExist(desc, "Missing discriminator value for {}", entityClass);
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
