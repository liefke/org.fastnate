package org.fastnate.generator.test.overrides;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.fastnate.generator.test.embedded.TestEmbeddedProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Contains some {@link AttributeOverride} and {@link AssociationOverride} for the {@link OverridesTest}.
 *
 * @author Tobias Liefke
 */
@Setter
@Getter
@Entity
@AssociationOverrides({
		@AssociationOverride(name = "otherEntity", joinTable = @JoinTable(name = "TestOverrideOtherEntity"), //
				joinColumns = @JoinColumn(name = "myColumn")),
		@AssociationOverride(name = "simpleEntities", joinTable = @JoinTable(name = "TestOverrideSimpleEntities", //
				joinColumns = @JoinColumn(name = "join_id"), inverseJoinColumns = @JoinColumn(name = "inverseJoin_id"))),
		@AssociationOverride(name = "stringList", joinTable = @JoinTable(name = "TestOverrideStringList", //
				joinColumns = @JoinColumn(name = "test_entity_id"))) })
@AttributeOverrides({ //
		@AttributeOverride(name = "simpleProperty", column = @Column(name = "overriddenSimpleProperty")),
		@AttributeOverride(name = "stringList", column = @Column(name = "test_string")) })
public class OverrideEntity extends ToOverrideEntity {

	@Embedded
	@AttributeOverride(name = "description", column = @Column(name = "overriddenDescription"))
	@AssociationOverride(name = "otherEntity", joinColumns = @JoinColumn(name = "testOverrideOtherNode"))
	private TestEmbeddedProperties embedded;

}
