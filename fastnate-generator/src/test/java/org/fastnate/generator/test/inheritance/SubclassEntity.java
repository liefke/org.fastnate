package org.fastnate.generator.test.inheritance;

/**
 * The properties for all superclass test entities to have one similar test in {@link InheritanceTest}.
 *
 * @author Tobias Liefke
 */
public interface SubclassEntity extends SuperclassEntity {

	/**
	 * The description of the entity, defined in the sub class in the entity hierarchy.
	 *
	 * @return the description
	 */
	String getDescription();

}
