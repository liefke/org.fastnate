package org.fastnate.generator.test.date;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
