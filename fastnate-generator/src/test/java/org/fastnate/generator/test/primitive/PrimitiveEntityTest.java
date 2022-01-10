package org.fastnate.generator.test.primitive;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

/**
 * Tests that primitive properties are written correctly.
 *
 * @author Tobias Liefke
 */
public class PrimitiveEntityTest extends AbstractEntitySqlGeneratorTest {

	private static final float MINIMUM_FLOAT_PRECISION = 0.000001f;

	/**
	 * Tests that converted properties are written.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testConvertedProperties() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Converter");

		final int rgba = 0x04010203;
		testEntity.setColor(new Color(rgba, true));
		write(testEntity);

		// Check that the color is written correctly
		final PrimitiveTestEntity result = findSingleResult(
				"SELECT e FROM PrimitiveTestEntity e WHERE e.color = coalesce(" + rgba + ')',
				PrimitiveTestEntity.class);
		assertThat(result.getColor()).isEqualTo(testEntity.getColor());
	}

	/**
	 * Tests to write enum properties in an entity.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testEnumProperties() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Enums");

		testEntity.setOrdinalEnum(TestEnum.one);

		testEntity.setStringEnum(TestEnum.two);

		write(testEntity);

		// Test equalness
		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);
		assertThat(result.getOrdinalEnum()).isSameAs(TestEnum.one);
		assertThat(result.getStringEnum()).isSameAs(TestEnum.two);
	}

	/**
	 * Tests to write BLOBs and CLOBs.
	 *
	 * Expected to fail with PostgreSQL.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testLobs() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Lobs");

		testEntity.setLobChars("Many \r\n Characters".toCharArray());
		testEntity.setLobBytes("\1\2\3\4\5\6\7\t\r\n\b\f\u0027 Bytes".getBytes("ISO-8859-1"));

		write(testEntity);

		// Test equalness
		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);
		assertThat(result.getName()).isEqualTo(testEntity.getName());

		assertThat(result.getLobChars()).isEqualTo(testEntity.getLobChars());
		assertThat(result.getLobBytes()).isEqualTo(testEntity.getLobBytes());
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
		final double testDouble = -0.98765432109876543210E20;
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
		assertThat(result.getTestFloat()).isEqualTo(testEntity.getTestFloat(),
				Assertions.withPrecision(MINIMUM_FLOAT_PRECISION));
		assertThat(result.getTestDouble()).isEqualTo(testEntity.getTestDouble());
	}

	/**
	 * Tests to write primitive properties in an entity.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testSerializableProperties() throws IOException {
		final PrimitiveTestEntity testEntity = new PrimitiveTestEntity("Test Serializables");

		testEntity.setSerializable(new SerTestObject("st", 2));

		write(testEntity);

		// Test equalness
		final PrimitiveTestEntity result = findSingleResult(PrimitiveTestEntity.class);
		assertThat(result.getSerializable()).isEqualTo(testEntity.getSerializable());
		assertThat(result.getSerializable().getStr()).isEqualTo(testEntity.getSerializable().getStr());
		assertThat(result.getSerializable().getNum()).isEqualTo(testEntity.getSerializable().getNum());
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