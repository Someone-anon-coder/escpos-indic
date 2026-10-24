/*
 * File:      ReceiptGoldenTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Compares the sample receipts with approved byte streams and their readable listings.
 * Owns:      ASCII receipt exact everywhere; Hindi receipt listing everywhere, bytes exact on the reference platform.
 * Uses:      SampleReceipts, StreamListing, GoldenFiles.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-06
 */
package io.github.someoneanoncoder.escposindic.layout;

import static org.assertj.core.api.Assumptions.assumeThat;

import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.testing.GoldenFiles;
import io.github.someoneanoncoder.escposindic.testing.GoldenTest;
import io.github.someoneanoncoder.escposindic.testing.SampleReceipts;
import io.github.someoneanoncoder.escposindic.testing.StreamListing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@GoldenTest
class ReceiptGoldenTest {

  @ParameterizedTest
  @EnumSource(PaperWidth.class)
  @DisplayName("the ASCII receipt with QR and barcodes is byte-identical on every platform")
  void shouldMatchGolden_whenAsciiReceipt(PaperWidth paper) {
    byte[] bytes = SampleReceipts.ascii(paper);
    String name = "receipt/ascii-" + paper.name().toLowerCase(java.util.Locale.ROOT);
    GoldenFiles.assertBinary(name, bytes);
    GoldenFiles.assertText(name, StreamListing.describe(bytes));
  }

  @ParameterizedTest
  @EnumSource(PaperWidth.class)
  @DisplayName("the Hindi receipt has the same commands and image sizes on every platform")
  void shouldMatchListing_whenIndicReceipt(PaperWidth paper) {
    byte[] bytes = SampleReceipts.indic(paper);
    String name = "receipt/indic-" + paper.name().toLowerCase(java.util.Locale.ROOT);
    String listing = StreamListing.describe(bytes);
    if (GoldenFiles.isReferencePlatform()) {
      GoldenFiles.assertBinary(name, bytes);
      GoldenFiles.assertText(name, listing);
    } else {
      assumeThat(listing).isNotEmpty();
      String expected = StreamListing.describe(GoldenFiles.readBinary(name));
      org.assertj.core.api.Assertions.assertThat(structureOf(listing))
          .isEqualTo(structureOf(expected));
    }
  }

  /** Drops the black-dot counts, which vary with text rendering, and keeps the rest. */
  private static String structureOf(String listing) {
    return listing.replaceAll(" blackDots=\\d+", "");
  }
}
