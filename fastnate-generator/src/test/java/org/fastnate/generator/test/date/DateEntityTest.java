package org.fastnate.generator.test.date;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.fastnate.generator.RelativeDate;
import org.fastnate.generator.test.AbstractEntitySqlGeneratorTest;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Tests that temporal properties are written correctly.
 *
 * @author Tobias Liefke
 */
@Slf4j
public class DateEntityTest extends AbstractEntitySqlGeneratorTest {

	/**
	 * Tests to write properties with absolute dates.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testAbsoluteDates() throws IOException {
		final DateTestEntity testEntity = new DateTestEntity();

		final long oneHour = DateUtils.MILLIS_PER_HOUR;
		final long oneDay = DateUtils.MILLIS_PER_DAY;
		final long time = oneDay + oneHour;
		testEntity.setDate(new Date(time));
		testEntity.setTime(new Date(time));
		testEntity.setTimestamp(new Date(time));

		write(testEntity);

		final DateTestEntity result = findSingleResult(DateTestEntity.class);

		// Check that only the parts of the date are written
		assertThat(result.getDate()).isEqualTo(DateUtils.truncate(testEntity.getDate(), Calendar.DATE));

		// Check that the correct time is written
		assertThat(result.getTime()).isEqualTo(new Date(oneHour));

		// Ignore the millis for timestamp comparison
		assertThat(new Date(
				result.getTimestamp().getTime() - result.getTimestamp().getTime() % DateUtils.MILLIS_PER_SECOND))
						.isEqualTo(testEntity.getTimestamp());
	}

	/**
	 * Tests to write properties with reference dates.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testReferenceDates() throws IOException {
		final DateTestEntity testEntity = new DateTestEntity();

		testEntity.setDate(RelativeDate.TODAY);
		testEntity.setTimestamp(RelativeDate.NOW);

		final long waitSeconds = 5;
		try {
			log.info("Wait for {} seconds to distinguish 'CURRENT_TIME' and _now_...", waitSeconds);
			Thread.sleep(waitSeconds * DateUtils.MILLIS_PER_SECOND);
		} catch (final InterruptedException e) {
			// Ignore
		}

		final long writeTime = System.currentTimeMillis();
		write(testEntity);

		final DateTestEntity result = findSingleResult(DateTestEntity.class);

		// Today is today
		assertThat(result.getDate()).isEqualTo(DateUtils.truncate(new Date(), Calendar.DATE));

		// The timestamp is nearly the time of writing (and not the time of generation)
		assertThat(Math.abs(result.getTimestamp().getTime() - writeTime)).as("Deviation from now")
				.isLessThan(2 * DateUtils.MILLIS_PER_SECOND);
	}

	/**
	 * Tests to write properties with relative dates.
	 *
	 * @throws IOException
	 *             if the generator throws one
	 */
	@Test
	public void testRelativeDates() throws IOException {
		final DateTestEntity testEntity = new DateTestEntity();

		final int days = 31;
		testEntity.setDate(new RelativeDate(RelativeDate.TODAY, -days, RelativeDate.DAYS));

		final int hours = 2;
		testEntity.setTimestamp(new RelativeDate(RelativeDate.NOW, hours, RelativeDate.HOURS));

		final long writeTime = System.currentTimeMillis();
		write(testEntity);

		final DateTestEntity result = findSingleResult(DateTestEntity.class);

		// The date is correct
		assertThat(result.getDate()).isEqualTo(DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -days));

		// The timestamp is nearly the time of writing (and not the time of generation)
		assertThat(Math.abs(result.getTimestamp().getTime() - writeTime - hours * DateUtils.MILLIS_PER_HOUR))
				.as("Deviation from two hours from now").isLessThan(2 * DateUtils.MILLIS_PER_SECOND);
	}

}