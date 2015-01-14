package org.fastnate.generator.provider;

/**
 * Encapsulates implementation details of Hibernate as JPA provider.
 *
 * @author Tobias Liefke
 */
public class HibernateProvider implements JpaProvider {

	@Override
	public String getDefaultSequence() {
		return "hibernate_sequence";
	}

	@Override
	public boolean isJoinedDiscriminatorNeeded() {
		return false;
	}

}
