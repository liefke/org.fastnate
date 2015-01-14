package org.fastnate.generator.test.inheritance;

/**
 * The properties for all superclass test entities to have one similar test in {@link InheritanceTest}.
 *
 * @author Tobias Liefke
 */
public interface SuperclassEntity {

	/**
	 * The name of the entity, defined in the root class of the entity hierarchy.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * The property that was defined in {@link MappedSuperclassTestEntity}.
	 *
	 * @return the super property
	 */
	String getSuperProperty();

}
