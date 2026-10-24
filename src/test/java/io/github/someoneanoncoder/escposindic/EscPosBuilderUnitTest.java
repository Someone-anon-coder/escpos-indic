/*
 * File:      EscPosBuilderUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks every ESC/POS command against bytes written by hand from the command reference.
 * Owns:      Expected bytes for init, align, bold, underline, size, feed, spacing, cut, drawer, text.
 * Uses:      EscPosBuilder, Hex.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.Hex;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class EscPosBuilderUnitTest {

  private static String hex(EscPosBuilder b) {
    return Hex.text(b.build());
  }

  @Test
  @DisplayName("empty builder has no bytes")
  void shouldBeEmpty_whenNothingAdded() {
    assertThat(EscPosBuilder.create().build()).isEmpty();
    assertThat(EscPosBuilder.create().length()).isZero();
  }

  @Test
  @DisplayName("ESC @ initialises")
  void shouldEmitEscAt_whenInitialize() {
    assertThat(hex(EscPosBuilder.create().initialize())).isEqualTo("1B 40");
  }

  @ParameterizedTest
  @CsvSource({"LEFT,1B 61 00", "CENTER,1B 61 01", "RIGHT,1B 61 02"})
  @DisplayName("ESC a n sets alignment")
  void shouldEmitEscA_whenAlign(Alignment alignment, String expected) {
    assertThat(hex(EscPosBuilder.create().align(alignment))).isEqualTo(expected);
  }

  @Test
  @DisplayName("ESC E n sets bold")
  void shouldEmitEscE_whenBold() {
    assertThat(hex(EscPosBuilder.create().bold(true).bold(false))).isEqualTo("1B 45 01 1B 45 00");
  }

  @ParameterizedTest
  @CsvSource({"0,1B 2D 00", "1,1B 2D 01", "2,1B 2D 02"})
  @DisplayName("ESC - n sets underline")
  void shouldEmitEscMinus_whenUnderline(int dots, String expected) {
    assertThat(hex(EscPosBuilder.create().underline(dots))).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "1,1,1D 21 00",
    "2,1,1D 21 10",
    "1,2,1D 21 01",
    "2,2,1D 21 11",
    "3,5,1D 21 24",
    "8,8,1D 21 77"
  })
  @DisplayName("GS ! n puts width in the high nibble and height in the low nibble")
  void shouldEmitGsBang_whenSize(int width, int height, String expected) {
    assertThat(hex(EscPosBuilder.create().size(width, height))).isEqualTo(expected);
  }

  @Test
  @DisplayName("ESC d n, ESC J n, ESC 3 n and ESC 2")
  void shouldEmitFeedAndSpacing_whenCalled() {
    assertThat(hex(EscPosBuilder.create().feed(3))).isEqualTo("1B 64 03");
    assertThat(hex(EscPosBuilder.create().feed(255))).isEqualTo("1B 64 FF");
    assertThat(hex(EscPosBuilder.create().feedUnits(24))).isEqualTo("1B 4A 18");
    assertThat(hex(EscPosBuilder.create().lineSpacing(30))).isEqualTo("1B 33 1E");
    assertThat(hex(EscPosBuilder.create().lineSpacingDefault())).isEqualTo("1B 32");
  }

  @Test
  @DisplayName("GS V 65/66 0 feeds to the cutting position then cuts")
  void shouldEmitGsV_whenCut() {
    assertThat(hex(EscPosBuilder.create().cut(CutMode.FULL))).isEqualTo("1D 56 41 00");
    assertThat(hex(EscPosBuilder.create().cut(CutMode.PARTIAL))).isEqualTo("1D 56 42 00");
  }

  @Test
  @DisplayName("ESC p m t1 t2 uses 2 ms units")
  void shouldEmitEscP_whenDrawerKick() {
    assertThat(hex(EscPosBuilder.create().drawerKick(DrawerPin.PIN_2))).isEqualTo("1B 70 00 32 32");
    assertThat(hex(EscPosBuilder.create().drawerKick(DrawerPin.PIN_5, 200, 400)))
        .isEqualTo("1B 70 01 64 C8");
    assertThat(hex(EscPosBuilder.create().drawerKick(DrawerPin.PIN_2, 510, 0)))
        .isEqualTo("1B 70 00 FF 00");
  }

  @Test
  @DisplayName("odd drawer milliseconds round up")
  void shouldRoundUp_whenDrawerMillisAreOdd() {
    assertThat(hex(EscPosBuilder.create().drawerKick(DrawerPin.PIN_2, 1, 3)))
        .isEqualTo("1B 70 00 01 02");
  }

  @Test
  @DisplayName("text is ASCII bytes and line adds LF")
  void shouldEmitAscii_whenText() {
    assertThat(hex(EscPosBuilder.create().text("Hi 1"))).isEqualTo("48 69 20 31");
    assertThat(hex(EscPosBuilder.create().line("A"))).isEqualTo("41 0A");
    assertThat(hex(EscPosBuilder.create().newLine())).isEqualTo("0A");
    assertThat(hex(EscPosBuilder.create().text(" ~"))).isEqualTo("20 7E");
  }

  @Test
  @DisplayName("commands chain in order and build returns a copy")
  void shouldKeepOrder_whenChained() {
    EscPosBuilder b =
        EscPosBuilder.create().initialize().align(Alignment.CENTER).bold(true).line("SHOP");
    byte[] first = b.build();
    first[0] = 0;
    assertThat(hex(b)).isEqualTo("1B 40 1B 61 01 1B 45 01 53 48 4F 50 0A");
    assertThat(b.length()).isEqualTo(13);
  }

  @Test
  @DisplayName("out-of-range parameters are refused")
  void shouldRefuse_whenParameterOutOfRange() {
    assertThatThrownBy(() -> EscPosBuilder.create().underline(3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EscPosBuilder.create().underline(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EscPosBuilder.create().size(0, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("widthMultiple");
    assertThatThrownBy(() -> EscPosBuilder.create().size(1, 9))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("heightMultiple");
    assertThatThrownBy(() -> EscPosBuilder.create().feed(256))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EscPosBuilder.create().feed(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EscPosBuilder.create().feedUnits(256))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EscPosBuilder.create().lineSpacing(256))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EscPosBuilder.create().drawerKick(DrawerPin.PIN_2, 511, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("onMillis");
    assertThatThrownBy(() -> EscPosBuilder.create().drawerKick(DrawerPin.PIN_2, 0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("offMillis");
  }

  @Test
  @DisplayName("boundary values are accepted")
  void shouldAccept_whenParameterOnBoundary() {
    assertThat(hex(EscPosBuilder.create().feed(0))).isEqualTo("1B 64 00");
    assertThat(hex(EscPosBuilder.create().lineSpacing(255))).isEqualTo("1B 33 FF");
    assertThat(hex(EscPosBuilder.create().feedUnits(255))).isEqualTo("1B 4A FF");
  }

  @Test
  @DisplayName("null arguments are refused")
  void shouldRefuseNull_whenArgumentMissing() {
    assertThatNullPointerException().isThrownBy(() -> EscPosBuilder.create().align(null));
    assertThatNullPointerException().isThrownBy(() -> EscPosBuilder.create().cut(null));
    assertThatNullPointerException().isThrownBy(() -> EscPosBuilder.create().drawerKick(null));
    assertThatNullPointerException().isThrownBy(() -> EscPosBuilder.create().text(null));
  }
}
