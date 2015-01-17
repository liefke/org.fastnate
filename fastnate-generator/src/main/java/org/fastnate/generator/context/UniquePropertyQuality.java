package org.fastnate.generator.context;



/**
 * Possible values for {@link GeneratorContext#setUniquePropertyQuality}.
 */
public enum UniquePropertyQuality {
	/** Only required primitives are allowed. */
	onlyRequiredPrimitives {

		@Override
		public boolean isAllowed(final Property<?, ?> property) {
			return property instanceof PrimitiveProperty && property.isRequired();
		}

	},
	/** Only required primitives and required entity references and are allowed. */
	onlyRequired {

		@Override
		public boolean isAllowed(final Property<?, ?> property) {
			return (property instanceof PrimitiveProperty || property instanceof EntityProperty)
				&& property.isRequired();
		}
	},
	/** Only primitives are allowed (even optional). */
	onlyPrimitives {

		@Override
		public boolean isAllowed(final Property<?, ?> property) {
			return property instanceof PrimitiveProperty;
		}

	},

	/** All primitives or entity references are allowed. */
	all {

		@Override
		public boolean isAllowed(final Property<?, ?> property) {
			return property instanceof PrimitiveProperty || property instanceof EntityProperty;
		}

	};

	/**
	 * Finds the best quality that matches the given property.
	 * 
	 * @param property
	 *            the property to check
	 * @return the quality with the lowest ordinal, that matches the given property, or {@code null} if the property is
	 *         never allowed
	 */
	public static final UniquePropertyQuality getMatchingQuality(final Property<?, ?> property) {
		if (property instanceof PrimitiveProperty) {
			if (property.isRequired()) {
				return onlyRequiredPrimitives;
			}
			return onlyPrimitives;
		} else if (property instanceof EntityProperty) {
			if (property.isRequired()) {
				return onlyRequired;
			}
			return all;
		}
		return null;
	}

	/**
	 * Indicates which type of property is allowed for this quality.
	 * 
	 * @param property
	 *            the property to check
	 * @return {@code true} if that property is allowed as unique property
	 */
	public abstract boolean isAllowed(final Property<?, ?> property);
}