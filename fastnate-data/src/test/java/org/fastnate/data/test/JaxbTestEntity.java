package org.fastnate.data.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.fastnate.data.xml.JaxbImporter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A entity to test the {@link JaxbImporter}.
 *
 * @author Tobias Liefke
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class JaxbTestEntity {

	@Id
	@XmlTransient
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@XmlAttribute
	private String name;

	@XmlValue
	private String content;

}
