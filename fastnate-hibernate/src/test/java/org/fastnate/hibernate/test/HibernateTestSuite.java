package org.fastnate.hibernate.test;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs all generator tests with Hibernate, even those from the fastnate-generator-test module.
 *
 * @author Tobias Liefke
 */
@Suite
@SelectPackages({ "org.fastnate.generator.test", "org.fastnate.hibernate.test.any" })
public class HibernateTestSuite {

	// Nothings specific to implement

}
