package org.fastnate.generator;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Used for date calculation when writing entities.
 *
 * Examples:
 * <dl>
 * <dt>Write "now" as property of a bean:</dt>
 * <dd><code>bean.setDate(RelativeDate.NOW)</code></dd>
 * <dt>Write "today" as property of a bean:</dt>
 * <dd><code>bean.setDate(RelativeDate.TODAY)</code></dd>
 * <dt>Write "yesterday" as property of a bean:</dt>
 * <dd><code>bean.setDate(new RelativeDate(RelativeDate.TODAY, - DateUtils.MILLIS_PER_DAY))</code></dd>
 * </dl>
 *
 * @author Tobias Liefke
 */
public class RelativeDate extends Date {

	/** The precision of a difference between a reference date and a date property. */
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	public static final class Precision {

		/** The name of the unit of this precision. */
		private final String unit;

		/** The precision of the unit in milliseconds. */
		private final long millis;

		@Override
		public String toString() {
			return this.unit;
		}

	}

	/** Marker for {@link RelativeDate#NOW} and {@link RelativeDate#TODAY}. */
	public static final class ReferenceDate extends Date {

		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance of {@link ReferenceDate}.
		 *
		 * Only used for our constants.
		 *
		 * @param timeInMillis
		 *            the time in milliseconds
		 */
		ReferenceDate(final long timeInMillis) {
			super(timeInMillis);
		}

		@Override
		public void setDate(final int date) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

		@Override
		public void setHours(final int hours) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

		@Override
		public void setMinutes(final int minutes) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

		@Override
		public void setMonth(final int month) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

		@Override
		public void setSeconds(final int seconds) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

		@Override
		public void setTime(final long time) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

		@Override
		public void setYear(final int year) {
			throw new UnsupportedOperationException("Can't change a reference date");
		}

	}

	private static final long serialVersionUID = 1L;

	/** Represents the constant for writing the "today" function to SQL. */
	public static final ReferenceDate TODAY = new ReferenceDate(System.currentTimeMillis() - 2);

	/** Represents the constant for writing the "now" function to SQL. */
	public static final ReferenceDate NOW = new ReferenceDate(System.currentTimeMillis() - 1);

	/** Used for adding milliseconds to a reference date. */
	public static final Precision MILLISECONDS = new Precision("MILLISECOND", 1);

	/** Used for adding seconds to a reference date. */
	public static final Precision SECONDS = new Precision("SECOND", DateUtils.MILLIS_PER_SECOND);

	/** Used for adding minutes to a reference date. */
	public static final Precision MINUTES = new Precision("MINUTE", DateUtils.MILLIS_PER_MINUTE);

	/** Used for adding hours to a reference date. */
	public static final Precision HOURS = new Precision("HOUR", DateUtils.MILLIS_PER_HOUR);

	/** Used for adding days to a reference date. */
	public static final Precision DAYS = new Precision("DAY", DateUtils.MILLIS_PER_DAY);

	/** Used for adding weeks to a reference date. */
	public static final Precision WEEKS = new Precision("WEEK", 7 * DateUtils.MILLIS_PER_DAY);

	/** Used for adding years to a reference date. */
	public static final Precision YEARS = new Precision("YEAR", 365 * DateUtils.MILLIS_PER_DAY);

	/** All known precisions, from the smallest to the biggest. */
	private static final Precision[] PRECISIONS = { MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS, WEEKS, YEARS };

	/** The date that is the base for this date. Only the difference to this date is written to the SQL file. */
	@Getter
	private final ReferenceDate referenceDate;

	/**
	 * Creates a new instance with the given millis as time. Uses {@link #NOW} as reference date.
	 *
	 * @param timeInMillis
	 *            the time in milliseconds. Only the difference to {@link #NOW} is written to SQL
	 */
	public RelativeDate(final long timeInMillis) {
		super(timeInMillis);
		this.referenceDate = NOW;
	}

	/**
	 * Creates a new instance with the given millis as time and the given reference date.
	 *
	 * @param referenceDate
	 *            the reference date
	 * @param deltaInMillis
	 *            the difference to the reference date in milliseconds
	 */
	public RelativeDate(final ReferenceDate referenceDate, final long deltaInMillis) {
		super(deltaInMillis + referenceDate.getTime());
		this.referenceDate = referenceDate;
	}

	/**
	 * Creates a new difference to a given reference date.
	 *
	 * @param referenceDate
	 *            the reference date
	 * @param delta
	 *            the difference to the reference date
	 * @param precision
	 *            the unit of the delta, one of the constants defined above
	 */
	public RelativeDate(final ReferenceDate referenceDate, final long delta, final Precision precision) {
		super(referenceDate.getTime() + delta * precision.getMillis());
		this.referenceDate = referenceDate;
	}

	/**
	 * The difference of the time to the {@link #referenceDate}.
	 *
	 * @return the difference in milliseconds
	 */
	public long getDifference() {
		return getTime() - this.referenceDate.getTime();
	}

	/**
	 * Finds the highest possible unit of the difference between the reference date and this date.
	 *
	 * @return the highest possible unit to use without loosing information
	 */
	public Precision getPrecision() {
		final long difference = getTime() - this.referenceDate.getTime();
		Precision previousPrecision = PRECISIONS[0];
		for (int i = 1; i < PRECISIONS.length; i++) {
			final Precision precision = PRECISIONS[i];
			if (difference % precision.getMillis() != 0) {
				break;
			}
			previousPrecision = precision;
		}
		return previousPrecision;
	}

}
