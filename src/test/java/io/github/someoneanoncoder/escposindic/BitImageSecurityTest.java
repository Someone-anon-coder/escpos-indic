/*
 * File:      BitImageSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves untrusted sizes and data cannot exhaust memory or corrupt images.
 * Owns:      Size limits, wrong lengths, out-of-range pixel access, threshold range.
 * Uses:      BitImage.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SecurityTest
class BitImageSecurityTest {

  @Test
  @DisplayName("sizes beyond the limits are refused before any allocation")
  void shouldRefuse_whenSizeOutOfRange() {
    assertThatThrownBy(() -> BitImage.blank(0, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.blank(1, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.blank(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.blank(BitImage.MAX_WIDTH + 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.blank(1, BitImage.MAX_HEIGHT + 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.blank(Integer.MAX_VALUE, Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> BitImage.blank(BitImage.MAX_WIDTH, 1));
    assertThatNoException().isThrownBy(() -> BitImage.blank(1, BitImage.MAX_HEIGHT));
  }

  @Test
  @DisplayName("generate and fromImage check sizes too")
  void shouldRefuse_whenGeneratedOrConvertedSizeOutOfRange() {
    assertThatThrownBy(() -> BitImage.generate(0, 1, (x, y) -> true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.generate(1, BitImage.MAX_HEIGHT + 1, (x, y) -> true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                BitImage.fromImage(
                    new BufferedImage(BitImage.MAX_WIDTH + 1, 1, BufferedImage.TYPE_BYTE_BINARY),
                    128))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("packed data of the wrong length is refused")
  void shouldRefuse_whenPackedLengthIsWrong() {
    assertThatThrownBy(() -> BitImage.of(8, 2, new byte[1]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.of(8, 1, new byte[2]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.of(0, 1, new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.of(BitImage.MAX_WIDTH + 1, 1, new byte[257]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.of(8, 1, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("pixel access outside the image is refused")
  void shouldRefuse_whenPixelOutside() {
    BitImage img = BitImage.blank(8, 2);
    assertThatThrownBy(() -> img.isBlack(8, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> img.isBlack(0, 2)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> img.isBlack(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  @DisplayName("threshold outside 1 to 255 is refused")
  void shouldRefuse_whenThresholdOutOfRange() {
    BufferedImage src = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    assertThatThrownBy(() -> BitImage.fromImage(src, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.fromImage(src, 256))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> BitImage.fromImage(src, 1));
    assertThatNoException().isThrownBy(() -> BitImage.fromImage(src, 255));
  }

  @Test
  @DisplayName("composition cannot exceed the size limits")
  void shouldRefuse_whenCompositionTooLarge() {
    BitImage wide = BitImage.blank(BitImage.MAX_WIDTH, 1);
    assertThatThrownBy(() -> BitImage.sideBySide(List.of(wide, wide)))
        .isInstanceOf(IllegalArgumentException.class);
    BitImage tall = BitImage.blank(1, BitImage.MAX_HEIGHT);
    assertThatThrownBy(() -> BitImage.stack(List.of(tall, tall)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
