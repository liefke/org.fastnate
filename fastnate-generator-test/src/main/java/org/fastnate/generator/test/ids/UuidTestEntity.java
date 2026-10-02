package org.fastnate.generator.test.ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.fastnate.generator.context.UuidGenerator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An entity that uses a UUID for generating its primary key.
 *
 * @author Tobias Liefke
 */
@Getter
@Entity
@NoArgsConstructor
public class UuidTestEntity extends IdTestEntity<UuidTestEntity> {

	@Id
	@Column(length = UuidGenerator.UUID_LENGTH)
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Setter
	@ManyToOne
	private UuidTestEntity other;

	/**
	 * Creates a new instance of {@link UuidTestEntity}.
	 *
	 * @param name
	 *            the name of the entity
	 */
	public UuidTestEntity(final String name) {
		super(name);
	}

}
