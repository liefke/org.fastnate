/**
 * Package for testing globally defined {@link org.hibernate.annotations.AnyMetaDef}
 */
@AnyMetaDefs({ @AnyMetaDef(name = "AnyMetaDefInPackage", idType = "long", metaType = "string", metaValues = {
		@MetaValue(targetEntity = SimpleTestEntity.class, value = "ST"), //
		@MetaValue(targetEntity = AnyContainer.class, value = "AC") }) })
package org.fastnate.generator.test.any;

import org.fastnate.generator.test.SimpleTestEntity;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.AnyMetaDefs;
import org.hibernate.annotations.MetaValue;
