/*
 * File:      TextWrapPropertyTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks wrapping never overflows and never loses words.
 * Owns:      Line length bound and word content preserved.
 * Uses:      TextWrap.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.someoneanoncoder.escposindic.testing.PropertyTest;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

@PropertyTest
class TextWrapPropertyTest {

  @Property
  void shouldNeverOverflowOrLoseLetters_whenWrapping(
      @ForAll("words") String text, @ForAll @IntRange(min = 1, max = 48) int width) {
    List<String> lines = TextWrap.lines(text, width);
    assertThat(lines).isNotEmpty().allMatch(l -> l.length() <= width);
    assertThat(String.join("", lines).replace(" ", "")).isEqualTo(text.replace(" ", ""));
  }

  @Provide
  Arbitrary<String> words() {
    return Arbitraries.strings().withCharRange('a', 'e').withChars(' ').ofMaxLength(150);
  }
}
