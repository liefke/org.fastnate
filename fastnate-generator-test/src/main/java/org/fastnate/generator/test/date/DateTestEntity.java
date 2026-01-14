package org.fastnate.generator.test.date;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.fastnate.generator.DefaultValue;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to test writing of times and dates.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateTestEntity {

	@Id
	@GeneratedValue
	private Long id;

	/** Legacy dates */

	@Temporal(TemporalType.DATE)
	@Column(name = "dateColumn")
	@DefaultValue("CURRENT_TIMESTAMP")
	private Date date;

	@Temporal(TemporalType.TIME)
	@Column(name = "timeColumn")
	private Date time;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "timestampColumn")
	private Date timestamp;

	@Temporal(TemporalType.TIMESTAMP)
	@DefaultValue("CURRENT_TIMESTAMP")
	private Date defaultDateNow;

	@Temporal(TemporalType.TIMESTAMP)
	@DefaultValue("2000-01-01T01:02:03.456+0200")
	private Date defaultDate2000;

	/** Java8 date objects */

	@Column(name = "localDateColumn")
	private LocalDate localDate;

	@Column(name = "localTimeColumn")
	private LocalTime localTime;

	@Column(name = "localDateTimeColumn")
	private LocalDateTime localDateTime;

	private Duration duration;

	@Override
	public boolean equals(final Object obj) {
		return this.id == null ? this == obj
				: obj instanceof DateTestEntity && this.id.equals(((DateTestEntity) obj).id);
	}

	@Override
	public int hashCode() {
		return this.id == null ? super.hashCode() : this.id.hashCode();
	}

	@Override
	public String toString() {
		return "DateTestEntity: " + (this.id == null ? '@' + hashCode() : this.id);
	}

}
