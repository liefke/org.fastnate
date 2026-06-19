package org.fastnate.eclipselink.test;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs all generator tests with EclipseLink, even those from the fastnate-generator-test module.
 *
 * @author Tobias Liefke
 */
@Suite
@SelectPackages({ "org.fastnate.generator.test", "org.fastnate.hibernate.test.any" })
public class EclipseLinkTestSuite {

	// Nothings specific to implement

}
