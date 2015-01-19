package org.fastnate.generator.test.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
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
		testEntity.getEntityMap().put(new SimpleTestEntity(firstKey), new SimpleTestEntity(firstValue));
		final String secondKey = "Second Key";
		final String secondValue = "Second Value";
		testEntity.getEntityMap().put(new SimpleTestEntity(secondKey), new SimpleTestEntity(secondValue));

		write(testEntity);

		final MapsTestEntity result = findSingleResult(MapsTestEntity.class);
		assertThat(result.getEntityMap()).hasSameSizeAs(testEntity.getEntityMap());
		final Iterator<Map.Entry<SimpleTestEntity, SimpleTestEntity>> entryIterator = result.getEntityMap().entrySet()
				.iterator();
		final Map.Entry<SimpleTestEntity, SimpleTestEntity> first = entryIterator.next();
		final Map.Entry<SimpleTestEntity, SimpleTestEntity> second = entryIterator.next();
		assertThat(first.getKey().getName()).isIn(firstKey, secondKey);
		final boolean firstIsFirst = first.getKey().getName().equals(firstKey);
		if (firstIsFirst) {
			assertThat(first.getValue().getName()).isEqualTo(firstValue);
			assertThat(second.getKey().getName()).isEqualTo(secondKey);
			assertThat(second.getValue().getName()).isEqualTo(secondValue);
		} else {
			assertThat(first.getValue().getName()).isEqualTo(secondValue);
			assertThat(second.getKey().getName()).isEqualTo(firstKey);
			assertThat(second.getValue().getName()).isEqualTo(firstValue);
		}
	}

}
