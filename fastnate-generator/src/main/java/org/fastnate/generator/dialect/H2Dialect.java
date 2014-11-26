package org.fastnate.generator.dialect;

/**
 * Handles H2 specific conversions.
 *
 * @see <a href="http://www.h2database.com/html/grammar.html">H2 - SQL Grammar</a>
 * @author Tobias Liefke
 */
public class H2Dialect extends GeneratorDialect {

	@Override
	public String createBlobExpression(final byte[] blob) {
		return createHexBlobExpression("'", blob, "'");
	}

}