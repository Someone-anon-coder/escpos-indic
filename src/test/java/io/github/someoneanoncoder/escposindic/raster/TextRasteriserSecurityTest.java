/*
 * File:      TextRasteriserSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves untrusted text and sizes are refused or bounded.
 * Owns:      Character limits, unsupported glyphs, control characters, size limits.
 * Uses:      TextRasteriser.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic.raster;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SecurityTest
class TextRasteriserSecurityTest {

  private final TextRasteriser r = TextRasteriser.of(TestFonts.devanagari(24f), PaperWidth.MM58);

  @ParameterizedTest
  @ValueSource(strings = {"\u001B@", "a\u0000b", "tab\there", "cr\rhere", "del\u007F", "\u0085"})
  @DisplayName("control characters other than line feed are refused")
  void shouldRefuse_whenControlCharacter(String text) {
    assertThatThrownBy(() -> r.rasterise(text, Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("characters the font lacks are refused instead of printing boxes")
  void shouldRefuse_whenFontCannotDisplay() {
    assertThatThrownBy(() -> r.rasterise("日本", Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("index 0");
    assertThatThrownBy(() -> r.rasterise("क日", Alignment.LEFT)).hasMessageContaining("index 1");
  }

  @Test
  @DisplayName("empty, null and over-long text are refused")
  void shouldRefuse_whenTextEmptyNullOrTooLong() {
    assertThatThrownBy(() -> r.rasterise("", Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNullPointerException().isThrownBy(() -> r.rasterise(null, Alignment.LEFT));
    assertThatNullPointerException().isThrownBy(() -> r.rasterise("क", null));
    assertThatThrownBy(
            () -> r.rasterise("क".repeat(TextRasteriser.MAX_CHARACTERS + 1), Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> r.rasterise("क".repeat(40), Alignment.LEFT));
  }

  @Test
  @DisplayName("text that would need an image taller than the limit is refused")
  void shouldRefuse_whenImageWouldBeTooTall() {
    String paragraphs = "क\n".repeat(900);
    assertThatThrownBy(() -> r.rasterise(paragraphs, Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tall");
  }

  @Test
  @DisplayName("width and threshold are range checked")
  void shouldRefuse_whenWidthOrThresholdOutOfRange() {
    assertThatThrownBy(() -> r.rasterise("क", 0, Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> r.rasterise("क", BitImage.MAX_WIDTH + 1, Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> r.withThreshold(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> r.withThreshold(256)).isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> r.withThreshold(1));
    assertThatNoException().isThrownBy(() -> r.withThreshold(255));
    assertThatNoException().isThrownBy(() -> r.rasterise("क", BitImage.MAX_WIDTH, Alignment.LEFT));
    assertThatNoException().isThrownBy(() -> r.rasterise("क", 1, Alignment.LEFT));
  }

  @Test
  @DisplayName("null font or paper is refused")
  void shouldRefuseNull_whenFontOrPaperMissing() {
    assertThatNullPointerException().isThrownBy(() -> TextRasteriser.of(null, PaperWidth.MM58));
    assertThatNullPointerException()
        .isThrownBy(() -> TextRasteriser.of(TestFonts.devanagari(24f), null));
  }
}
