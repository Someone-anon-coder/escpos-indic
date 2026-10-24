/*
 * File:      SampleReceipts.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Builds the sample receipts used by the golden tests and the README example test.
 * Owns:      A pure-ASCII receipt with QR and barcodes and a Hindi/mixed receipt, at both paper widths.
 * Uses:      ReceiptWriter, TextRasteriser, BarcodeRaster, TestFonts.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-06
 */
package io.github.someoneanoncoder.escposindic.testing;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.CutMode;
import io.github.someoneanoncoder.escposindic.DrawerPin;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.barcode.BarcodeRaster;
import io.github.someoneanoncoder.escposindic.barcode.QrLevel;
import io.github.someoneanoncoder.escposindic.layout.Column;
import io.github.someoneanoncoder.escposindic.layout.ReceiptWriter;
import io.github.someoneanoncoder.escposindic.raster.TextRasteriser;

/** Fixed sample receipts. All names, numbers and the GSTIN are invented. */
public final class SampleReceipts {

  /** UPI link printed as a QR on the samples. */
  public static final String UPI_LINK =
      "upi://pay?pa=sample@bank&pn=Sample%20Hardware&am=1593.00&cu=INR&tn=Bill%20A%2F26-27%2F000001";

  private SampleReceipts() {}

  /**
   * A receipt of plain ASCII text, a QR code and two barcodes. No text rendering is involved, so
   * its bytes are the same on every operating system.
   *
   * @param paper the paper width
   * @return the byte stream
   */
  public static byte[] ascii(PaperWidth paper) {
    int cols = paper.columns();
    ReceiptWriter w = ReceiptWriter.asciiOnly(paper);
    w.commands().initialize();
    w.align(Alignment.CENTER);
    w.commands().bold(true).size(2, 2);
    w.line("SAMPLE HARDWARE");
    w.commands().size(1, 1).bold(false);
    w.line("12 Market Road, Sample Town").line("GSTIN 00AAAAA0000A0Z0 (sample)");
    w.align(Alignment.LEFT).rule('-');
    w.line("Bill A/26-27/000001").line("Date 24/10/2026");
    w.rule('-');
    w.row(Column.left(cols - 18, "Item"), Column.right(4, "Qty"), Column.right(14, "Amount"));
    w.rule('-');
    w.row(
        Column.left(cols - 18, "Cement 50kg"), Column.right(4, "2"), Column.right(14, "1,300.00"));
    w.row(
        Column.left(cols - 18, "PVC pipe 25mm"), Column.right(4, "4"), Column.right(14, "180.00"));
    w.row(Column.left(cols - 18, "Binding wire"), Column.right(4, "1"), Column.right(14, "113.00"));
    w.rule('-');
    w.commands().bold(true);
    w.row(Column.left(cols - 14, "TOTAL"), Column.right(14, "1,593.00"));
    w.commands().bold(false);
    w.align(Alignment.CENTER).line("Scan to pay");
    w.image(BarcodeRaster.qr(UPI_LINK, QrLevel.M, paper.dots() - 96));
    w.image(BarcodeRaster.code128("A/26-27/000001", 2, 50));
    w.line("A/26-27/000001");
    w.image(BarcodeRaster.ean13("590123412345", 2, 50));
    w.line("5901234123457");
    w.line("Thank you");
    w.commands().feed(2).cut(CutMode.PARTIAL).drawerKick(DrawerPin.PIN_2);
    return w.build();
  }

  /**
   * A Hindi receipt with mixed rows, the rupee sign and a QR code. Its text is drawn by Java2D, so
   * its bytes are exact only on the reference platform.
   *
   * @param paper the paper width
   * @return the byte stream
   */
  public static byte[] indic(PaperWidth paper) {
    int cols = paper.columns();
    TextRasteriser raster =
        TextRasteriser.of(TestFonts.latin(22f), paper).withFallback(TestFonts.devanagari(24f));
    TextRasteriser title =
        TextRasteriser.of(TestFonts.latin(34f), paper).withFallback(TestFonts.devanagari(36f));
    ReceiptWriter w = ReceiptWriter.of(raster);
    w.commands().initialize();
    w.align(Alignment.CENTER).image(title.rasterise("शर्मा हार्डवेयर", Alignment.CENTER));
    w.line("12 बाज़ार रोड, नमूना नगर");
    w.align(Alignment.LEFT).rule('-');
    w.row(Column.left(cols - 18, "बिल A/26-27/000001"), Column.right(18, "24/10/2026"));
    w.rule('-');
    w.row(Column.left(cols - 20, "सामान"), Column.right(6, "मात्रा"), Column.right(14, "रकम"));
    w.rule('-');
    w.row(
        Column.left(cols - 20, "सीमेंट 50 किलो बोरी"),
        Column.right(6, "2"),
        Column.right(14, "₹1,300.00"));
    w.row(
        Column.left(cols - 20, "PVC पाइप 25 मिमी x 3 मी"),
        Column.right(6, "4"),
        Column.right(14, "₹180.00"));
    w.row(
        Column.left(cols - 20, "बाइंडिंग तार 1 किलो"),
        Column.right(6, "1"),
        Column.right(14, "₹113.00"));
    w.rule('-');
    w.row(Column.left(cols - 14, "कुल"), Column.right(14, "₹1,593.00"));
    w.align(Alignment.CENTER).line("भुगतान के लिए स्कैन करें");
    w.image(BarcodeRaster.qr(UPI_LINK, QrLevel.M, paper.dots() - 96));
    w.line("धन्यवाद, फिर पधारिए");
    w.commands().feed(2).cut(CutMode.PARTIAL);
    return w.build();
  }
}
