/*
 * File:      PaperWidthUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks the paper width constants.
 * Owns:      Dot widths, columns and bytes per row.
 * Uses:      PaperWidth.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class PaperWidthUnitTest {

  @Test
  @DisplayName("58 mm is 384 dots, 32 columns, 48 bytes")
  void shouldDescribe58mm_whenAsked() {
    assertThat(PaperWidth.MM58.dots()).isEqualTo(384);
    assertThat(PaperWidth.MM58.columns()).isEqualTo(32);
    assertThat(PaperWidth.MM58.bytesPerRow()).isEqualTo(48);
  }

  @Test
  @DisplayName("80 mm is 576 dots, 48 columns, 72 bytes")
  void shouldDescribe80mm_whenAsked() {
    assertThat(PaperWidth.MM80.dots()).isEqualTo(576);
    assertThat(PaperWidth.MM80.columns()).isEqualTo(48);
    assertThat(PaperWidth.MM80.bytesPerRow()).isEqualTo(72);
  }
}
