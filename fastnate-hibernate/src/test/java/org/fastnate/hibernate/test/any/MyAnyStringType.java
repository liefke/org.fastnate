package org.fastnate.hibernate.test.any;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.persistence.DiscriminatorType;

import org.fastnate.generator.test.SimpleTestEntity;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

/**
 * Tests declaration of {@link AnyDiscriminatorValue} with {@link DiscriminatorType#STRING}.
 *
 * @author Tobias Liefke
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@AnyKeyJavaClass(Long.class)
@AnyDiscriminator(DiscriminatorType.STRING)
@AnyDiscriminatorValue(entity = SimpleTestEntity.class, discriminator = "ST")
@AnyDiscriminatorValue(entity = AnyContainer.class, discriminator = "AC")
public @interface MyAnyStringType {

	// This annotation is empty

}