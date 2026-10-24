/*
 * File:      EscPosBuilderImageUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks the GS v 0 raster command bytes against the command reference.
 * Owns:      Header layout, little-endian sizes, banding of tall images.
 * Uses:      EscPosBuilder, BitImage.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.someoneanoncoder.escposindic.testing.Hex;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class EscPosBuilderImageUnitTest {

  @Test
  @DisplayName("GS v 0 m xL xH yL yH then the packed rows")
  void shouldEmitRasterHeader_whenImagePrinted() {
    BitImage img = BitImage.of(8, 2, new byte[] {(byte) 0xFF, 0x00});
    byte[] bytes = EscPosBuilder.create().image(img).build();
    assertThat(Hex.text(bytes)).isEqualTo("1D 76 30 00 01 00 02 00 FF 00");
  }

  @Test
  @DisplayName("width in bytes and height are little-endian 16-bit values")
  void shouldWriteLittleEndianSizes_whenImageIsWide() {
    BitImage wide = BitImage.blank(BitImage.MAX_WIDTH, 1);
    byte[] bytes = EscPosBuilder.create().image(wide).build();
    assertThat(Hex.text(Arrays.copyOf(bytes, 8))).isEqualTo("1D 76 30 00 00 01 01 00");
    assertThat(bytes).hasSize(8 + 256);
  }

  @Test
  @DisplayName("a 384 dot row is 48 bytes (xL = 0x30)")
  void shouldUse48Bytes_whenFiftyEightMillimetreRow() {
    byte[] bytes = EscPosBuilder.create().image(BitImage.blank(384, 1)).build();
    assertThat(Hex.text(Arrays.copyOf(bytes, 8))).isEqualTo("1D 76 30 00 30 00 01 00");
  }

  @Test
  @DisplayName("tall images go out in bands of 255 rows, each with its own header")
  void shouldSplitIntoBands_whenImageIsTall() {
    BitImage tall = BitImage.blank(8, 300);
    byte[] bytes = EscPosBuilder.create().image(tall).build();
    assertThat(bytes).hasSize(8 + 255 + 8 + 45);
    assertThat(Hex.text(Arrays.copyOf(bytes, 8))).isEqualTo("1D 76 30 00 01 00 FF 00");
    assertThat(Hex.text(Arrays.copyOfRange(bytes, 263, 271))).isEqualTo("1D 76 30 00 01 00 2D 00");
  }

  @Test
  @DisplayName("exactly 255 rows is a single band and 256 rows is two")
  void shouldSplitAtBoundary_whenRowsAre255Or256() {
    assertThat(EscPosBuilder.create().image(BitImage.blank(8, 255)).build()).hasSize(8 + 255);
    assertThat(EscPosBuilder.create().image(BitImage.blank(8, 256)).build())
        .hasSize(8 + 255 + 8 + 1);
  }

  @Test
  @DisplayName("band data carries the right rows")
  void shouldCarryRowsInOrder_whenBanded() {
    byte[] rows = new byte[256];
    rows[0] = 1;
    rows[254] = 2;
    rows[255] = 3;
    byte[] bytes = EscPosBuilder.create().image(BitImage.of(8, 256, rows)).build();
    assertThat(bytes[8]).isEqualTo((byte) 1);
    assertThat(bytes[8 + 254]).isEqualTo((byte) 2);
    assertThat(bytes[8 + 255 + 8]).isEqualTo((byte) 3);
  }

  @Test
  @DisplayName("null image is refused")
  void shouldRefuseNull_whenImageMissing() {
    assertThatNullPointerException().isThrownBy(() -> EscPosBuilder.create().image(null));
  }
}
