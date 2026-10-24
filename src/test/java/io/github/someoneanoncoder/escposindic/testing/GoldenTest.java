/*
 * File:      GoldenTest.java
 * Module:    escpos-indic
 * Layer:     testing
 * Purpose:   Meta-annotation that tags a test as category 'golden'.
 * Owns:      JUnit 5 tag "golden".
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

/** Output equals an approved file. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("golden")
public @interface GoldenTest {}
