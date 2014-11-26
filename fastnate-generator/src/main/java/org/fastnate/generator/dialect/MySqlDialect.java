package org.fastnate.generator.dialect;

/**
 * Handles MySQL specific conversions.
 *
 * Attention: MySQL is currently not fully supported. Especially
 * <ul>
 * <li>time and date functions</li>
 * </ul>
 * are not covered.
 *
 * @see <a href="http://dev.mysql.com/doc/">MySQL - Reference Manuals</a>
 *
 * @author Tobias Liefke
 */
public final class MySqlDialect extends GeneratorDialect {

	@Override
	public boolean isInsertSelectSameTableSupported() {
		return false;
	}
}