/*
 * File:      RasterGoldenTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Compares rasterised text and barcodes with approved PNG pictures.
 * Owns:      Hindi line, mixed row, QR, Code 128 and EAN-13 at both paper widths.
 * Uses:      TextRasteriser, BarcodeRaster, ReceiptWriter, GoldenFiles.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-06
 */
package io.github.someoneanoncoder.escposindic.raster;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.barcode.BarcodeRaster;
import io.github.someoneanoncoder.escposindic.barcode.QrLevel;
import io.github.someoneanoncoder.escposindic.testing.GoldenFiles;
import io.github.someoneanoncoder.escposindic.testing.GoldenTest;
import io.github.someoneanoncoder.escposindic.testing.SampleReceipts;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@GoldenTest
class RasterGoldenTest {

  private static TextRasteriser rasteriser(PaperWidth paper) {
    return TextRasteriser.of(TestFonts.latin(22f), paper).withFallback(TestFonts.devanagari(24f));
  }

  @ParameterizedTest
  @EnumSource(PaperWidth.class)
  @DisplayName("a wrapped Hindi paragraph looks as approved")
  void shouldMatchGolden_whenHindiParagraph(PaperWidth paper) {
    BitImage img =
        rasteriser(paper)
            .rasterise(
                "हिन्दी में रसीद: क्षत्रिय, ज्ञान, श्री, द्वार, कुल रकम ₹1,250.00 धन्यवाद, फिर आइए।",
                Alignment.LEFT);
    GoldenFiles.assertPng(
        "raster/hindi-" + paper.name().toLowerCase(java.util.Locale.ROOT), img, false);
  }

  @Test
  @DisplayName("centred and right aligned Hindi lines look as approved")
  void shouldMatchGolden_whenAlignedHindi() {
    TextRasteriser r = rasteriser(PaperWidth.MM58);
    GoldenFiles.assertPng(
        "raster/hindi-centre-58", r.rasterise("धन्यवाद", Alignment.CENTER), false);
    GoldenFiles.assertPng(
        "raster/hindi-right-58", r.rasterise("कुल ₹1,593.00", Alignment.RIGHT), false);
  }

  @Test
  @DisplayName("a mixed Latin and Devanagari cell row looks as approved")
  void shouldMatchGolden_whenMixedCells() {
    TextRasteriser r = rasteriser(PaperWidth.MM58);
    BitImage row =
        BitImage.sideBySide(
            List.of(
                r.rasterise("सीमेंट 50 किलो बोरी", 12 * 12, Alignment.LEFT),
                r.rasterise("2", 6 * 12, Alignment.RIGHT),
                r.rasterise("₹1,300.00", 14 * 12, Alignment.RIGHT)));
    GoldenFiles.assertPng("raster/mixed-row-58", row, false);
  }

  @Test
  @DisplayName("QR and barcodes are pixel-exact on every platform")
  void shouldMatchGolden_whenBarcodes() {
    GoldenFiles.assertPng(
        "barcode/qr-upi-58", BarcodeRaster.qr(SampleReceipts.UPI_LINK, QrLevel.M, 288), true);
    GoldenFiles.assertPng(
        "barcode/qr-upi-80", BarcodeRaster.qr(SampleReceipts.UPI_LINK, QrLevel.M, 480), true);
    GoldenFiles.assertPng(
        "barcode/code128-bill", BarcodeRaster.code128("A/26-27/000001", 2, 50), true);
    GoldenFiles.assertPng("barcode/ean13-sample", BarcodeRaster.ean13("590123412345", 2, 50), true);
  }
}
