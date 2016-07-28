package org.fastnate.generator.test.primitive;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to test primitive properties.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrimitiveTestEntity {

	/**
	 * Name query to find entity by :name.
	 */
	public static final String NQ_ENTITY_BY_NAME = "singularEntityByName";

	private static final int NAME_LENGTH = 30;

	@Id
	@GeneratedValue
	private Long id;

	@Column(length = NAME_LENGTH, unique = true)
	@NotNull
	@Size(min = 1)
	private String name;

	@Transient
	private String transient1;

	private transient String transient2;

	private String description;

	private char testChar;

	private boolean testBoolean;

	private byte testByte;

	private short testShort;

	private int testInt;

	private long testLong;

	private float testFloat;

	private double testDouble;

	@Temporal(TemporalType.DATE)
	@Column(name = "dateColumn")
	private Date date;

	@Temporal(TemporalType.TIME)
	@Column(name = "timeColumn")
	private Date time;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "timestampColumn")
	private Date timestamp;

	@Lob
	private char[] lobChars;

	@Lob
	// For PostgreSQL: @Column(columnDefinition = "BYTEA")
	private byte[] lobBytes;

	private SerializableTestObject serializale;

	@Enumerated
	private TestEnum ordinalEnum;

	@Enumerated(EnumType.STRING)
	private TestEnum stringEnum;

	/**
	 * Creates a new instance of {@link PrimitiveTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public PrimitiveTestEntity(final String name) {
		this.name = name;
		// PostgreSQL denies "null" characters and H2 trims whitespace characters
		this.testChar = 'x';
	}

	@Override
	public boolean equals(final Object obj) {
		return this.id == null ? this == obj
				: obj instanceof PrimitiveTestEntity && this.id.equals(((PrimitiveTestEntity) obj).id);
	}

	@Override
	public int hashCode() {
		return this.id == null ? super.hashCode() : this.id.hashCode();
	}

	@Override
	public String toString() {
		return "TestSingularEntity: " + (this.id == null ? '@' + hashCode() : this.id);
	}

}
