package org.fastnate.generator;

import java.util.Date;

import lombok.Getter;

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

	/**
	 * Represents the constant for writing the "now" function to SQL.
	 */
	public static final ReferenceDate NOW = new ReferenceDate(System.currentTimeMillis() - 1);

	/**
	 * Represents the constant for writing the "today" function to SQL.
	 */
	public static final ReferenceDate TODAY = new ReferenceDate(System.currentTimeMillis() - 2);

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
	 *            the difference to the reference date, which is written to SQL
	 */
	public RelativeDate(final ReferenceDate referenceDate, final long deltaInMillis) {
		super(deltaInMillis + referenceDate.getTime());
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

}
