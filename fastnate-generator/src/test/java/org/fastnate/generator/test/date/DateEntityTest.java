package org.fastnate.generator.test.date;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
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

		final LocalDate localDate = LocalDate.of(2020, 2, 29);
		testEntity.setLocalDate(localDate);
		final LocalTime localTime = LocalTime.of(22, 56, 54);
		testEntity.setLocalTime(localTime);
		final LocalDateTime localDateTime = LocalDateTime.of(2021, 12, 31, 23, 53, 52);
		testEntity.setLocalDateTime(localDateTime);
		final Duration duration = Duration.ofSeconds(321);
		testEntity.setDuration(duration);
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

		// Check Java8 times
		assertThat(result.getLocalDate()).isEqualTo(localDate);
		assertThat(result.getLocalTime()).isEqualTo(localTime);
		assertThat(result.getLocalDateTime()).isEqualTo(localDateTime);
		assertThat(result.getDuration()).isEqualTo(duration);

		// Check that the default is written correctly
		try {
			assertThat(new Date(result.getDefaultDate2000().getTime())).isEqualTo(
					new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("2000-01-01T01:02:03.456+02:00"));
		} catch (final ParseException e) {
			throw new AssertionError(e);
		}
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

		final int days = 123;
		testEntity.setDate(new RelativeDate(RelativeDate.TODAY, -days, RelativeDate.DAYS));

		final int hours = 2;
		testEntity.setTimestamp(new RelativeDate(RelativeDate.NOW, hours, RelativeDate.HOURS));

		final long writeTime = System.currentTimeMillis();
		write(testEntity);

		final DateTestEntity result = findSingleResult(DateTestEntity.class);

		// The date is correct
		assertThat(result.getDate()).isEqualTo(DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -days));

		final long maxDeviation = 2 * DateUtils.MILLIS_PER_SECOND;
		// The timestamp is nearly the time of writing (and not the time of generation)
		assertThat(Math.abs(result.getTimestamp().getTime() - writeTime - hours * DateUtils.MILLIS_PER_HOUR))
				.as("Deviation from two hours from now").isLessThan(maxDeviation);

		// Check that the default is written correctly
		assertThat(Math.abs(result.getDefaultDateNow().getTime() - writeTime)).as("Deviation from now")
				.isLessThan(maxDeviation);
	}

}