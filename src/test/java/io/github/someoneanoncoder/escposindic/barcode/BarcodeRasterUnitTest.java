/*
 * File:      BarcodeRasterUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks QR, Code 128 and EAN-13 rasters by decoding them again.
 * Owns:      Round trips, sizes, scaling and known check digits.
 * Uses:      BarcodeRaster, BarcodeDecoder.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-04
 */
package io.github.someoneanoncoder.escposindic.barcode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BarcodeFormat;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.testing.BarcodeDecoder;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class BarcodeRasterUnitTest {

  private static final String UPI =
      "upi://pay?pa=shop@okbank&pn=Ramesh%20Store&am=118.00&cu=INR&tn=Bill%20A%2F26-27%2F000001";

  @ParameterizedTest
  @ValueSource(ints = {384, 576})
  @DisplayName("a UPI QR fits the paper, is square and decodes to the same text")
  void shouldDecodeUpiLink_whenQrFitsPaper(int paperDots) {
    BitImage img = BarcodeRaster.qr(UPI, QrLevel.M, paperDots);
    assertThat(img.width()).isEqualTo(img.height()).isLessThanOrEqualTo(paperDots);
    BarcodeDecoder.Decoded decoded = BarcodeDecoder.decode(img, BarcodeFormat.QR_CODE);
    assertThat(decoded.text()).isEqualTo(UPI);
    assertThat(decoded.format()).isEqualTo("QR_CODE");
  }

  @ParameterizedTest
  @EnumSource(QrLevel.class)
  @DisplayName("every error correction level decodes")
  void shouldDecode_whenAnyLevel(QrLevel level) {
    assertThat(
            BarcodeDecoder.decode(BarcodeRaster.qr("ABC-123", level, 384), BarcodeFormat.QR_CODE)
                .text())
        .isEqualTo("ABC-123");
  }

  @Test
  @DisplayName("modules are whole dots and as large as the width allows")
  void shouldScaleToLargestWholeModule_whenQr() {
    BitImage small = BarcodeRaster.qr("A", QrLevel.L, 300);
    // version 1 is 21 modules + 2 * 4 quiet zone = 29 modules; 300 / 29 = 10 dots per module
    assertThat(small.width()).isEqualTo(290);
    assertThat(BarcodeRaster.qr("A", QrLevel.L, 29).width()).isEqualTo(29);
    assertThat(BarcodeRaster.qr("A", QrLevel.L, 57).width()).isEqualTo(29);
    assertThat(BarcodeRaster.qr("A", QrLevel.L, 58).width()).isEqualTo(58);
  }

  @Test
  @DisplayName("quiet zone around the QR is white")
  void shouldHaveQuietZone_whenQr() {
    BitImage img = BarcodeRaster.qr("A", QrLevel.L, 29);
    for (int i = 0; i < 29; i++) {
      for (int edge = 0; edge < 4; edge++) {
        assertThat(img.isBlack(i, edge)).isFalse();
        assertThat(img.isBlack(edge, i)).isFalse();
        assertThat(img.isBlack(i, 28 - edge)).isFalse();
        assertThat(img.isBlack(28 - edge, i)).isFalse();
      }
    }
    assertThat(img.isBlack(4, 4)).isTrue();
  }

  @Test
  @DisplayName("Hindi text in a QR survives as UTF-8")
  void shouldDecodeHindi_whenQrHasUtf8() {
    assertThat(
            BarcodeDecoder.decode(
                    BarcodeRaster.qr("धन्यवाद", QrLevel.M, 384), BarcodeFormat.QR_CODE)
                .text())
        .isEqualTo("धन्यवाद");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  @DisplayName("Code 128 decodes at every module width and has the requested height")
  void shouldDecodeCode128_whenModuleWidthVaries(int module) {
    BitImage img = BarcodeRaster.code128("A/26-27/000001", module, 60);
    assertThat(img.height()).isEqualTo(60);
    BarcodeDecoder.Decoded decoded = BarcodeDecoder.decode(img, BarcodeFormat.CODE_128);
    assertThat(decoded.text()).isEqualTo("A/26-27/000001");
    assertThat(decoded.format()).isEqualTo("CODE_128");
  }

  @Test
  @DisplayName("Code 128 width grows in whole modules")
  void shouldScaleWidth_whenModuleDotsDoubles() {
    int one = BarcodeRaster.code128("12345678", 1, 10).width();
    assertThat(BarcodeRaster.code128("12345678", 2, 10).width()).isEqualTo(one * 2);
    assertThat(BarcodeRaster.code128("12345678", 3, 10).width()).isEqualTo(one * 3);
  }

  @Test
  @DisplayName("a 12 digit EAN-13 gets its check digit: 590123412345 becomes 5901234123457")
  void shouldAddCheckDigit_whenTwelveDigits() {
    BarcodeDecoder.Decoded decoded =
        BarcodeDecoder.decode(BarcodeRaster.ean13("590123412345", 2, 80), BarcodeFormat.EAN_13);
    assertThat(decoded.text()).isEqualTo("5901234123457");
    assertThat(decoded.format()).isEqualTo("EAN_13");
  }

  @Test
  @DisplayName("a 13 digit EAN-13 with a right check digit is accepted")
  void shouldAccept_whenThirteenDigitsWithRightCheck() {
    assertThat(
            BarcodeDecoder.decode(
                    BarcodeRaster.ean13("8901030865275", 3, 100), BarcodeFormat.EAN_13)
                .text())
        .isEqualTo("8901030865275");
  }

  @Test
  @DisplayName("bars are as tall as asked and the quiet zones are white")
  void shouldHaveHeightAndQuietZone_whenEan13() {
    BitImage img = BarcodeRaster.ean13("590123412345", 2, 40);
    assertThat(img.height()).isEqualTo(40);
    for (int y = 0; y < 40; y++) {
      assertThat(img.isBlack(0, y)).isFalse();
      assertThat(img.isBlack(img.width() - 1, y)).isFalse();
    }
    assertThat(img.isBlack(img.width() / 2, 0)).isEqualTo(img.isBlack(img.width() / 2, 39));
  }

  @Test
  @DisplayName(
      "a long content that once failed a block-based decoder decodes: 150 characters, level M")
  void shouldDecode_whenContentIsLongAndModulesAreBig() {
    String content =
        "vzSnyu6OcfNDgDknBpxJsTK6oQL1okMNfZVtQSpwTcKSBHf0or3zh30jggka7fnQd2TEuRjEKWcJdgSrZanVXfDygMwLfsIzuSqhZzyiLtfctMrSSDzznNC6QsqCQbmggxsrNHHDx1TSXFFR6ehJoDp1";
    BitImage img = BarcodeRaster.qr(content, QrLevel.M, 576);
    assertThat(img.width()).isEqualTo(570); // 57 modules of 10 dots, no ECI header
    assertThat(BarcodeDecoder.decode(img, BarcodeFormat.QR_CODE).text()).isEqualTo(content);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Sl83nbxvaNbWHGSQRtPOYc9sxG857XMQGVn",
        "4IPMOKfYSDfZTAaObekMZAN",
        "Y0ayjBpDocCv9HQQDgft"
      })
  @DisplayName("regression: Code 128 data that ZXing also reads as UPC-A or EAN-8 round-trips")
  void shouldDecodeCode128_whenAnotherFormatHidesInTheBars(String data) {
    BitImage img = BarcodeRaster.code128(data, 3, 40);
    BarcodeDecoder.Decoded decoded = BarcodeDecoder.decode(img, BarcodeFormat.CODE_128);
    assertThat(decoded.text()).isEqualTo(data);
    assertThat(decoded.format()).isEqualTo("CODE_128");
  }
}
