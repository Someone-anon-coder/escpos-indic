/*
 * File:      BitImageUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks bit packing, composition and conversion of BitImage.
 * Owns:      MSB-first packing, padding, threshold, side-by-side, stack, equality.
 * Uses:      BitImage.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class BitImageUnitTest {

  @Test
  @DisplayName("bit 7 of the first byte is the left-most dot")
  void shouldReadMostSignificantBitFirst_whenPacked() {
    BitImage img = BitImage.of(8, 1, new byte[] {(byte) 0b1000_0001});
    assertThat(img.isBlack(0, 0)).isTrue();
    assertThat(img.isBlack(1, 0)).isFalse();
    assertThat(img.isBlack(7, 0)).isTrue();
    assertThat(img.rowBytes()).isEqualTo(1);
  }

  @Test
  @DisplayName("rows are padded to whole bytes and padding bits are cleared")
  void shouldClearPadding_whenWidthIsNotMultipleOfEight() {
    BitImage img = BitImage.of(10, 2, new byte[] {(byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xC0});
    assertThat(img.rowBytes()).isEqualTo(2);
    assertThat(img.toBytes()).containsExactly(0xFF, 0xC0, 0x00, 0xC0);
    assertThat(img)
        .isEqualTo(BitImage.of(10, 2, new byte[] {(byte) 0xFF, (byte) 0xC0, 0, (byte) 0xC0}));
  }

  @Test
  @DisplayName("blank is white and sizes are reported")
  void shouldBeWhite_whenBlank() {
    BitImage img = BitImage.blank(9, 3);
    assertThat(img.width()).isEqualTo(9);
    assertThat(img.height()).isEqualTo(3);
    assertThat(img.toBytes()).containsOnly(0).hasSize(6);
    assertThat(img).hasToString("BitImage[9x3]");
  }

  @Test
  @DisplayName("of copies the array and toBytes returns a copy")
  void shouldNotShareArrays_whenCreatedOrRead() {
    byte[] source = {(byte) 0x80};
    BitImage img = BitImage.of(8, 1, source);
    source[0] = 0;
    img.toBytes()[0] = 0;
    assertThat(img.isBlack(0, 0)).isTrue();
  }

  @Test
  @DisplayName("rowsToBytes returns the requested band")
  void shouldSliceRows_whenBandRequested() {
    BitImage img = BitImage.of(8, 3, new byte[] {1, 2, 3});
    assertThat(img.rowsToBytes(1, 3)).containsExactly(2, 3);
    assertThatThrownBy(() -> img.rowsToBytes(2, 4)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  @DisplayName("fromImage turns dark dots black and light dots white at the threshold")
  void shouldThreshold_whenConvertingFromImage() {
    BufferedImage src = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
    src.setRGB(0, 0, 0x000000);
    src.setRGB(1, 0, 0x7F7F7F);
    src.setRGB(2, 0, 0xFFFFFF);
    BitImage img = BitImage.fromImage(src, 128);
    assertThat(img.isBlack(0, 0)).isTrue();
    assertThat(img.isBlack(1, 0)).isTrue();
    assertThat(img.isBlack(2, 0)).isFalse();
    assertThat(BitImage.fromImage(src, 64).isBlack(1, 0)).isFalse();
    assertThat(BitImage.fromImage(src, 127).isBlack(1, 0)).isFalse();
    assertThat(BitImage.fromImage(src, 128).isBlack(1, 0)).isTrue();
  }

  @Test
  @DisplayName("transparent dots count as white, half transparent black as mid grey")
  void shouldBlendWithWhite_whenAlphaIsPresent() {
    BufferedImage src = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
    src.setRGB(0, 0, 0x00000000);
    src.setRGB(1, 0, 0x80000000);
    src.setRGB(2, 0, 0xFF000000);
    BitImage img = BitImage.fromImage(src, 200);
    assertThat(img.isBlack(0, 0)).isFalse();
    assertThat(img.isBlack(1, 0)).isTrue();
    assertThat(img.isBlack(2, 0)).isTrue();
    assertThat(BitImage.fromImage(src, 100).isBlack(1, 0)).isFalse();
  }

  @Test
  @DisplayName("colour brightness uses luma weights")
  void shouldWeightChannels_whenColourDots() {
    BufferedImage src = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
    src.setRGB(0, 0, 0xFF0000); // luma 76
    src.setRGB(1, 0, 0x00FF00); // luma 149
    src.setRGB(2, 0, 0x0000FF); // luma 29
    BitImage img = BitImage.fromImage(src, 100);
    assertThat(img.isBlack(0, 0)).isTrue();
    assertThat(img.isBlack(1, 0)).isFalse();
    assertThat(img.isBlack(2, 0)).isTrue();
  }

  @Test
  @DisplayName("sideBySide places parts left to right and pads short ones")
  void shouldPlaceSideBySide_whenComposed() {
    BitImage a = BitImage.of(8, 1, new byte[] {(byte) 0xFF});
    BitImage b = BitImage.of(4, 2, new byte[] {(byte) 0x80, (byte) 0x40});
    BitImage c = BitImage.sideBySide(List.of(a, b));
    assertThat(c.width()).isEqualTo(12);
    assertThat(c.height()).isEqualTo(2);
    assertThat(c.isBlack(7, 0)).isTrue();
    assertThat(c.isBlack(7, 1)).isFalse();
    assertThat(c.isBlack(8, 0)).isTrue();
    assertThat(c.isBlack(9, 1)).isTrue();
    assertThat(c.isBlack(8, 1)).isFalse();
  }

  @Test
  @DisplayName("stack places parts top to bottom and pads narrow ones")
  void shouldStack_whenComposed() {
    BitImage a = BitImage.of(8, 1, new byte[] {(byte) 0xFF});
    BitImage b = BitImage.of(4, 1, new byte[] {(byte) 0x80});
    BitImage c = BitImage.stack(List.of(b, a));
    assertThat(c.width()).isEqualTo(8);
    assertThat(c.height()).isEqualTo(2);
    assertThat(c.isBlack(0, 0)).isTrue();
    assertThat(c.isBlack(5, 0)).isFalse();
    assertThat(c.isBlack(5, 1)).isTrue();
  }

  @Test
  @DisplayName("composition of nothing is refused")
  void shouldRefuse_whenNoParts() {
    assertThatThrownBy(() -> BitImage.sideBySide(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BitImage.stack(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("toBufferedImage draws black dots as black pixels")
  void shouldRoundTrip_whenConvertedToBufferedImageAndBack() {
    BitImage img = BitImage.of(10, 2, new byte[] {(byte) 0xA5, (byte) 0x40, 0x0F, (byte) 0x80});
    BufferedImage drawn = img.toBufferedImage();
    assertThat(drawn.getWidth()).isEqualTo(10);
    assertThat(drawn.getRGB(0, 0)).isEqualTo(0xFF000000);
    assertThat(drawn.getRGB(1, 0)).isEqualTo(0xFFFFFFFF);
    assertThat(BitImage.fromImage(drawn, 128)).isEqualTo(img);
  }

  @Test
  @DisplayName("equal pictures have equal hash codes, different ones differ")
  void shouldCompareByContent_whenEquals() {
    BitImage a = BitImage.of(8, 1, new byte[] {1});
    assertThat(a)
        .isEqualTo(BitImage.of(8, 1, new byte[] {1}))
        .hasSameHashCodeAs(BitImage.of(8, 1, new byte[] {1}));
    assertThat(a).isNotEqualTo(BitImage.of(8, 1, new byte[] {2}));
    assertThat(a).isNotEqualTo(BitImage.of(8, 2, new byte[] {1, 0}));
    assertThat(a).isNotEqualTo(BitImage.of(7, 1, new byte[] {0}));
    assertThat(a).isNotEqualTo("x");
  }
}
