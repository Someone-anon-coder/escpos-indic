/*
 * File:      TextWrapUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks word wrapping for native text.
 * Owns:      Greedy wrap, hard split, paragraphs, boundaries.
 * Uses:      TextWrap.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class TextWrapUnitTest {

  @Test
  @DisplayName("short text stays one line")
  void shouldKeepOneLine_whenTextFits() {
    assertThat(TextWrap.lines("hello world", 11)).containsExactly("hello world");
  }

  @Test
  @DisplayName("breaks at the last space that fits")
  void shouldBreakAtSpace_whenTextIsTooLong() {
    assertThat(TextWrap.lines("hello big world", 9)).containsExactly("hello big", "world");
    assertThat(TextWrap.lines("hello big world", 10)).containsExactly("hello big", "world");
    assertThat(TextWrap.lines("hello big world", 15)).containsExactly("hello big world");
    assertThat(TextWrap.lines("aa bb cc", 5)).containsExactly("aa bb", "cc");
  }

  @Test
  @DisplayName("a word longer than the width is cut into pieces")
  void shouldCutWord_whenWordIsLongerThanWidth() {
    assertThat(TextWrap.lines("abcdefghij", 4)).containsExactly("abcd", "efgh", "ij");
    assertThat(TextWrap.lines("ab abcdefgh", 4)).containsExactly("ab", "abcd", "efgh");
    assertThat(TextWrap.lines("abcdefgh x", 4)).containsExactly("abcd", "efgh", "x");
  }

  @Test
  @DisplayName("a rest of a cut word can share a line with the next word")
  void shouldContinueLine_whenCutWordLeavesRoom() {
    assertThat(TextWrap.lines("abcdef gh", 4)).containsExactly("abcd", "ef", "gh");
    assertThat(TextWrap.lines("abcdef g", 4)).containsExactly("abcd", "ef g");
  }

  @Test
  @DisplayName("line feeds start paragraphs and empty paragraphs give an empty line")
  void shouldSplitParagraphs_whenLineFeedPresent() {
    assertThat(TextWrap.lines("a\nb", 10)).containsExactly("a", "b");
    assertThat(TextWrap.lines("a\n\nb", 10)).containsExactly("a", "", "b");
    assertThat(TextWrap.lines("", 10)).containsExactly("");
  }

  @Test
  @DisplayName("repeated and edge spaces collapse at breaks")
  void shouldIgnoreExtraSpaces_whenWrapping() {
    assertThat(TextWrap.lines("  a   b  ", 10)).containsExactly("a b");
  }

  @Test
  @DisplayName("width below 1 is refused")
  void shouldRefuse_whenWidthIsZero() {
    assertThatThrownBy(() -> TextWrap.lines("a", 0)).isInstanceOf(IllegalArgumentException.class);
  }
}
