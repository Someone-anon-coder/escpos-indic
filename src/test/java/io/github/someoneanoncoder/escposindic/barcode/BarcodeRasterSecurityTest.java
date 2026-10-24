/*
 * File:      BarcodeRasterSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves oversized or malformed barcode input is refused without echoing the content.
 * Owns:      Length limits, character limits, size limits and error text.
 * Uses:      BarcodeRaster.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-04
 */
package io.github.someoneanoncoder.escposindic.barcode;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SecurityTest
class BarcodeRasterSecurityTest {

  @Test
  @DisplayName("QR content must be 1 to 1000 characters")
  void shouldRefuse_whenQrContentEmptyOrTooLong() {
    assertThatThrownBy(() -> BarcodeRaster.qr("", QrLevel.M, 384))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> BarcodeRaster.qr("x".repeat(BarcodeRaster.MAX_QR_CHARACTERS + 1), QrLevel.M, 576))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException()
        .isThrownBy(
            () -> BarcodeRaster.qr("x".repeat(BarcodeRaster.MAX_QR_CHARACTERS), QrLevel.L, 2048));
    assertThatNullPointerException().isThrownBy(() -> BarcodeRaster.qr(null, QrLevel.M, 384));
    assertThatNullPointerException().isThrownBy(() -> BarcodeRaster.qr("a", null, 384));
  }

  @Test
  @DisplayName("a QR that cannot fit even with 1-dot modules is refused")
  void shouldRefuse_whenQrDoesNotFit() {
    assertThatThrownBy(() -> BarcodeRaster.qr("x".repeat(500), QrLevel.H, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dots");
    assertThatThrownBy(() -> BarcodeRaster.qr("A", QrLevel.L, 28))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "tab\tin", "nul\u0000", "é", "धन्यवाद", "del\u007F"})
  @DisplayName("Code 128 data must be printable ASCII")
  void shouldRefuse_whenCode128DataIsNotPrintableAscii(String data) {
    assertThatThrownBy(() -> BarcodeRaster.code128(data, 2, 50))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Code 128 length, module and height limits")
  void shouldRefuse_whenCode128OutOfRange() {
    assertThatThrownBy(() -> BarcodeRaster.code128("x".repeat(61), 1, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> BarcodeRaster.code128("x".repeat(60), 1, 10));
    assertThatThrownBy(() -> BarcodeRaster.code128("a", 0, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BarcodeRaster.code128("a", 9, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BarcodeRaster.code128("a", 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BarcodeRaster.code128("a", 1, 513))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> BarcodeRaster.code128("a", 8, 512));
    assertThatNullPointerException().isThrownBy(() -> BarcodeRaster.code128(null, 1, 10));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "12345678901",
        "12345678901234",
        "12345678901a",
        "1234567890123",
        "５９０１２３４１２３４５"
      })
  @DisplayName("EAN-13 needs 12 digits or 13 digits with a right check digit")
  void shouldRefuse_whenEan13IsMalformed(String digits) {
    assertThatThrownBy(() -> BarcodeRaster.ean13(digits, 2, 50))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("EAN-13 module and height limits and null")
  void shouldRefuse_whenEan13OutOfRange() {
    assertThatThrownBy(() -> BarcodeRaster.ean13("590123412345", 0, 50))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BarcodeRaster.ean13("590123412345", 2, 513))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNullPointerException().isThrownBy(() -> BarcodeRaster.ean13(null, 2, 50));
  }

  @Test
  @DisplayName("error messages never repeat the content")
  void shouldNotEcho_whenRefusing() {
    String secret = "upi://pay?pa=secret@bank" + "x".repeat(1000);
    assertThatThrownBy(() -> BarcodeRaster.qr(secret, QrLevel.M, 384))
        .hasMessageNotContaining("secret");
    assertThatThrownBy(() -> BarcodeRaster.code128("secret\u0001", 2, 50))
        .hasMessageNotContaining("secret");
    assertThatThrownBy(() -> BarcodeRaster.qr("secret" + "x".repeat(400), QrLevel.H, 50))
        .hasMessageNotContaining("secret");
  }
}
