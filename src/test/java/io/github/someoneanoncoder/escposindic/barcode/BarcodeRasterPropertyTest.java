/*
 * File:      BarcodeRasterPropertyTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks every generated barcode decodes to its input.
 * Owns:      Round trip of random content.
 * Uses:      BarcodeRaster, BarcodeDecoder.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-04
 */
package io.github.someoneanoncoder.escposindic.barcode;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BarcodeFormat;
import io.github.someoneanoncoder.escposindic.testing.BarcodeDecoder;
import io.github.someoneanoncoder.escposindic.testing.PropertyTest;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;

@PropertyTest
class BarcodeRasterPropertyTest {

  @Property(tries = 40)
  void shouldRoundTripQr_whenContentIsText(
      @ForAll
          @net.jqwik.api.constraints.AlphaChars
          @net.jqwik.api.constraints.NumericChars
          @StringLength(min = 1, max = 200)
          String content,
      @ForAll QrLevel level) {
    assertThat(
            BarcodeDecoder.decode(BarcodeRaster.qr(content, level, 576), BarcodeFormat.QR_CODE)
                .text())
        .isEqualTo(content);
  }

  @Property(tries = 40)
  void shouldRoundTripCode128_whenDataIsAlphanumeric(
      @ForAll @net.jqwik.api.constraints.AlphaChars @NumericChars @StringLength(min = 1, max = 40)
          String data,
      @ForAll @IntRange(min = 2, max = 3) int module) {
    assertThat(
            BarcodeDecoder.decode(BarcodeRaster.code128(data, module, 40), BarcodeFormat.CODE_128)
                .text())
        .isEqualTo(data);
  }

  @Property(tries = 40)
  void shouldAddRightCheckDigit_whenTwelveDigits(
      @ForAll @NumericChars @StringLength(11) String rest) {
    // A leading 0 is reported by ZXing as UPC-A (12 digits), so start with 5.
    String digits = "5" + rest;
    String decoded =
        BarcodeDecoder.decode(BarcodeRaster.ean13(digits, 2, 50), BarcodeFormat.EAN_13).text();
    assertThat(decoded).startsWith(digits).hasSize(13);
    int sum = 0;
    for (int i = 0; i < 12; i++) {
      sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
    }
    assertThat(decoded.charAt(12) - '0').isEqualTo((10 - sum % 10) % 10);
  }
}
