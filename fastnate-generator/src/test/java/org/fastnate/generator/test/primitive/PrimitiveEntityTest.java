package org.fastnate.generator.test.primitive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that primitive properties are written correctly.
 *
 * @author Tobias Liefke
 */
public class PrimitiveEntityTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write BLOBs and CLOBs.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testLobs() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Lobs");

		testEntity.setManyCharacters("Many \r\n Characters".toCharArray());
		testEntity.setManyBytes("\0\1\2\3\4\5\6\7\t\r\n\b\f\u0027 Bytes".getBytes("ISO-8859-1"));

		write(testEntity);

		// Test equalness
		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);
		assertThat(result.getName()).isEqualTo(testEntity.getName());

		assertThat(result.getManyCharacters()).isEqualTo(testEntity.getManyCharacters());
		assertThat(result.getManyBytes()).isEqualTo(testEntity.getManyBytes());
	}

	/**
	 * Tests to write primitive properties in an entity.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testPrimitiveProperties() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Primitives");

		testEntity.setDescription("Test all primitive values and 'escaping'.\n\t\r\b\"New line\".\'");
		testEntity.setTestChar('\'');

		testEntity.setTestBoolean(true);

		testEntity.setTestByte(Byte.MIN_VALUE);
		testEntity.setTestShort(Short.MIN_VALUE);
		testEntity.setTestInt(Integer.MIN_VALUE);
		testEntity.setTestLong(Long.MIN_VALUE);

		final float testFloat = -0.987654321f;
		testEntity.setTestFloat(testFloat);
		final double testDouble = -0.98765432109876543210;
		testEntity.setTestDouble(testDouble);

		write(testEntity);

		// Test equalness
		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);
		assertThat(result.getName()).isEqualTo(testEntity.getName());
		assertThat(result.getDescription()).isEqualTo(testEntity.getDescription());
		assertThat(result.getTestChar()).isEqualTo(testEntity.getTestChar());
		assertThat(result.isTestBoolean()).isEqualTo(testEntity.isTestBoolean());
		assertThat(result.getTestByte()).isEqualTo(testEntity.getTestByte());
		assertThat(result.getTestShort()).isEqualTo(testEntity.getTestShort());
		assertThat(result.getTestInt()).isEqualTo(testEntity.getTestInt());
		assertThat(result.getTestLong()).isEqualTo(testEntity.getTestLong());
		assertThat(result.getTestFloat()).isEqualTo(testEntity.getTestFloat());
		assertThat(result.getTestDouble()).isEqualTo(testEntity.getTestDouble());
	}

	/**
	 * Tests to write temporal properties in an entity.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testTemporalProperties() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Temporals");

		final long oneHour = DateUtils.MILLIS_PER_HOUR;
		final long oneDay = DateUtils.MILLIS_PER_DAY;
		final long time = oneDay + oneHour;
		testEntity.setTestDate(new Date(time));
		testEntity.setTestTime(new Date(time));
		testEntity.setTestTimestamp(new Date(time));

		write(testEntity);

		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);

		// Check that only the parts of the date are written
		assertThat(result.getTestDate()).isEqualTo(DateUtils.truncate(testEntity.getTestDate(), Calendar.DATE));

		// Check that the time is correclty written
		assertThat(result.getTestTime()).isEqualTo(new Date(oneHour));

		// Ignore the millis for timestamp comparison
		assertThat(
				new Date(result.getTestTimestamp().getTime() - result.getTestTimestamp().getTime()
						% DateUtils.MILLIS_PER_SECOND)).isEqualTo(testEntity.getTestTimestamp());
	}

	/**
	 * Tests that transient properties are not written.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testTransientProperties() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Transients");

		testEntity.setTransient1("transient 1");
		testEntity.setTransient2("transient 2");

		write(testEntity);

		// Test equalness
		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);

		// Check that no transient fields are written
		assertThat(result.getTransient1()).isNull();
		assertThat(result.getTransient2()).isNull();
	}

}