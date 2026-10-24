/*
 * File:      SecurityTest.java
 * Module:    escpos-indic
 * Layer:     testing
 * Purpose:   Meta-annotation that tags a test as category 'security'.
 * Owns:      JUnit 5 tag "security".
 * Uses:      JUnit 5 @Tag.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3
 */
package io.github.someoneanoncoder.escposindic.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/** Untrusted input, injection, resource limits. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("security")
public @interface SecurityTest {}
