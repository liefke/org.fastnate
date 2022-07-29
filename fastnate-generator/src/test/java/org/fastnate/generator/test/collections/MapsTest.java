package org.fastnate.generator.test.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.data.MapEntry;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.fastnate.generator.test.SimpleTestEntity;
import org.junit.Test;

/**
 * Test maps in entities.
 *
 * @author Tobias Liefke
 */
public class MapsTest extends AbstractEntitySqlGeneratorTest {

	private static String getName(final Object value) {
		return value instanceof SimpleTestEntity ? ((SimpleTestEntity) value).getName() : value.toString();
	}

	private static <K, V> void testMap(final String firstKey, final String firstValue, final String secondKey,
			final String secondValue, final Map<K, V> resultMap, final Map<K, V> testMap) {
		assertThat(resultMap).hasSameSizeAs(testMap);
		final Iterator<Map.Entry<K, V>> entryIterator = resultMap.entrySet().iterator();
		final Map.Entry<K, V> first = entryIterator.next();
		final Map.Entry<K, V> second = entryIterator.next();
		assertThat(getName(first.getKey())).isIn(firstKey, secondKey);
		final boolean firstIsFirst = getName(first.getKey()).equals(firstKey);
		if (firstIsFirst) {
			assertThat(getName(first.getValue())).isEqualTo(firstValue);
			assertThat(getName(second.getKey())).isEqualTo(secondKey);
			assertThat(getName(second.getValue())).isEqualTo(secondValue);
		} else {
			assertThat(getName(first.getValue())).isEqualTo(secondValue);
			assertThat(getName(second.getKey())).isEqualTo(firstKey);
			assertThat(getName(second.getValue())).isEqualTo(firstValue);
		}
	}

	/**
	 * Tests to write maps with content of basic type.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testBasicMaps() throws IOException {
		final MapsTestEntity testEntity = new MapsTestEntity();

		final String firstKey = "First Key";
		final String firstValue = "First Value";
		testEntity.getStringMap().put(firstKey, firstValue);
		final String secondKey = "Second Key";
		final String secondValue = "Second Value";
		testEntity.getStringMap().put(secondKey, secondValue);

		final Date now = new Date();
		testEntity.getDateMap().put(now, 2);

		final Date tomorrow = new Date(now.getTime() + DateUtils.MILLIS_PER_DAY);
		testEntity.getDateMap().put(tomorrow, 0);

		write(testEntity);

		final MapsTestEntity result = findSingleResult(MapsTestEntity.class);
		assertThat(result.getStringMap()).containsOnly(MapEntry.entry(firstKey, firstValue),
				MapEntry.entry(secondKey, secondValue));

		assertThat(result.getDateMap()).containsOnly(MapEntry.entry(DateUtils.truncate(now, Calendar.DATE), 2),
				MapEntry.entry(DateUtils.truncate(tomorrow, Calendar.DATE), 0));
	}

	/**
	 * Tests to write maps with entities.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testEntityMaps() throws IOException {
		final MapsTestEntity testEntity = new MapsTestEntity();

		final String firstKey = "First Key";
		final String firstValue = "First Value";
		testEntity.getStringToEntityMap().put(firstKey, new SimpleTestEntity(firstValue));
		final String secondKey = "Second Key";
		final String secondValue = "Second Value";
		testEntity.getStringToEntityMap().put(secondKey, new SimpleTestEntity(secondValue));

		testEntity.getEntityToEntityMap().put(new SimpleTestEntity(firstKey), new SimpleTestEntity(firstValue));
		testEntity.getEntityToEntityMap().put(new SimpleTestEntity(secondKey), new SimpleTestEntity(secondValue));

		write(testEntity);

		final MapsTestEntity result = findSingleResult(MapsTestEntity.class);
		testMap(firstKey, firstValue, secondKey, secondValue, result.getEntityToEntityMap(),
				testEntity.getEntityToEntityMap());
		testMap(firstKey, firstValue, secondKey, secondValue, result.getStringToEntityMap(),
				testEntity.getStringToEntityMap());
	}

}
