/*
 * File:      ReadmeExamplesUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Runs the README examples so the documentation cannot rot.
 * Owns:      The four examples as executable code.
 * Uses:      The public API.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.someoneanoncoder.escposindic.barcode.BarcodeRaster;
import io.github.someoneanoncoder.escposindic.barcode.QrLevel;
import io.github.someoneanoncoder.escposindic.layout.Column;
import io.github.someoneanoncoder.escposindic.layout.ReceiptWriter;
import io.github.someoneanoncoder.escposindic.raster.TextRasteriser;
import io.github.someoneanoncoder.escposindic.testing.StreamListing;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import io.github.someoneanoncoder.escposindic.transport.FileSinkTransport;
import io.github.someoneanoncoder.escposindic.transport.PrintQueueTransport;
import io.github.someoneanoncoder.escposindic.transport.TcpTransport;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@UnitTest
class ReadmeExamplesUnitTest {

  @TempDir Path dir;

  @Test
  @DisplayName("example 1: plain ASCII receipt into a file")
  void shouldWriteAsciiReceipt_whenExampleOne() throws IOException {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.commands().initialize();
    w.align(Alignment.CENTER).line("SAMPLE HARDWARE");
    w.align(Alignment.LEFT).rule('-');
    w.row(Column.left(14, "Cement 50kg"), Column.right(4, "2"), Column.right(14, "1,300.00"));
    w.rule('-');
    w.commands().feed(2).cut(CutMode.PARTIAL);

    Path file = dir.resolve("receipt.bin");
    new FileSinkTransport(file, false).send(w.build());

    assertThat(StreamListing.describe(Files.readAllBytes(file)))
        .contains("LINE \"SAMPLE HARDWARE\"")
        .contains("LINE \"Cement 50kg      2      1,300.00\"")
        .endsWith("FEED_LINES 2\nCUT m=66 n=0\n");
  }

  @Test
  @DisplayName("example 2: Hindi and Marathi mixed with Latin text")
  void shouldMixScripts_whenExampleTwo() {
    Font latin = TestFonts.latin(22f);
    Font devanagari = TestFonts.devanagari(24f);
    TextRasteriser raster = TextRasteriser.of(latin, PaperWidth.MM80).withFallback(devanagari);

    ReceiptWriter w = ReceiptWriter.of(raster);
    w.align(Alignment.CENTER).line("धन्यवाद, फिर पधारिए");
    w.line("मराठी: पुन्हा या");
    w.align(Alignment.LEFT).row(Column.left(30, "सीमेंट 50 किलो"), Column.right(18, "₹1,300.00"));
    w.line("Thank you");

    String listing = StreamListing.describe(w.build());
    assertThat(listing.lines().filter(l -> l.startsWith("IMAGE bytesPerRow=72")).count())
        .isEqualTo(3);
    assertThat(listing).contains("LINE \"Thank you\"");
  }

  @Test
  @DisplayName("example 3: UPI QR code and barcodes")
  void shouldPrintBarcodes_whenExampleThree() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.align(Alignment.CENTER).line("Scan to pay");
    w.image(BarcodeRaster.qr("upi://pay?pa=shop@bank&am=118.00&cu=INR", QrLevel.M, 288));
    w.image(BarcodeRaster.code128("A/26-27/000001", 2, 50));
    w.image(BarcodeRaster.ean13("590123412345", 2, 50));

    assertThat(StreamListing.describe(w.build()).lines().filter(l -> l.startsWith("IMAGE")).count())
        .isGreaterThanOrEqualTo(3); // a QR taller than 255 rows goes out as two bands
  }

  @Test
  @DisplayName("example 4: the transports can be created")
  void shouldCreateTransports_whenExampleFour() {
    assertThat(TcpTransport.rawPrinter("192.168.1.50")).isNotNull();
    assertThat(PrintQueueTransport.named("POS-58", Duration.ofSeconds(15))).isNotNull();
    assertThat(PrintQueueTransport.queueNames()).isNotNull();
  }
}
