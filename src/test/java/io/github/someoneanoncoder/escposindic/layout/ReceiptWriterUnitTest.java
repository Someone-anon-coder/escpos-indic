/*
 * File:      ReceiptWriterUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks native versus raster choice, rows, rules and alignment handling.
 * Owns:      Byte-level expectations for ASCII rows, raster rows and left-edge handling.
 * Uses:      ReceiptWriter, Column, TextRasteriser.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.raster.TextRasteriser;
import io.github.someoneanoncoder.escposindic.testing.Hex;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class ReceiptWriterUnitTest {

  private static TextRasteriser rasteriser(PaperWidth paper) {
    return TextRasteriser.of(TestFonts.latin(22f), paper).withFallback(TestFonts.devanagari(24f));
  }

  private static String ascii(byte[] bytes) {
    return new String(bytes, StandardCharsets.ISO_8859_1);
  }

  @Test
  @DisplayName("an ASCII line is native text with a line feed")
  void shouldBeNative_whenLineIsAscii() {
    byte[] bytes = ReceiptWriter.asciiOnly(PaperWidth.MM58).line("TOTAL 100").build();
    assertThat(Hex.text(bytes)).isEqualTo("54 4F 54 41 4C 20 31 30 30 0A");
  }

  @Test
  @DisplayName("a long ASCII line wraps at the paper columns")
  void shouldWrap_whenAsciiLineIsLongerThanPaper() {
    String text = "word ".repeat(9).trim();
    byte[] bytes = ReceiptWriter.asciiOnly(PaperWidth.MM58).line(text).build();
    String printed = ascii(bytes);
    for (String l : printed.split("\n")) {
      assertThat(l.length()).isLessThanOrEqualTo(32);
    }
    assertThat(printed.split("\n")).hasSize(2);
    ReceiptWriter wide = ReceiptWriter.asciiOnly(PaperWidth.MM80);
    assertThat(ascii(wide.line(text).build()).split("\n")).hasSize(1);
  }

  @Test
  @DisplayName("a Devanagari line becomes a GS v 0 image as wide as the paper")
  void shouldBeRaster_whenLineIsDevanagari() {
    byte[] bytes = ReceiptWriter.of(rasteriser(PaperWidth.MM58)).line("धन्यवाद").build();
    assertThat(Hex.text(java.util.Arrays.copyOf(bytes, 6))).isEqualTo("1D 76 30 00 30 00");
  }

  @Test
  @DisplayName("the rupee sign makes a line raster")
  void shouldBeRaster_whenLineHasRupeeSign() {
    byte[] bytes = ReceiptWriter.of(rasteriser(PaperWidth.MM80)).line("Total ₹100.00").build();
    assertThat(Hex.text(java.util.Arrays.copyOf(bytes, 6))).isEqualTo("1D 76 30 00 48 00");
  }

  @Test
  @DisplayName("alignment is sent once and applied to native lines and raster lines")
  void shouldKeepAlignment_whenAlignSet() {
    ReceiptWriter w = ReceiptWriter.of(rasteriser(PaperWidth.MM58));
    w.align(Alignment.CENTER).line("Hi");
    assertThat(Hex.text(w.build())).isEqualTo("1B 61 01 48 69 0A");
    int ink =
        firstBlackColumn(
            ReceiptWriter.of(rasteriser(PaperWidth.MM58))
                .align(Alignment.CENTER)
                .line("क")
                .build());
    assertThat(ink).isBetween(150, 200);
  }

  /** Left-most black column of the first image in the stream (after the 3-byte ESC a). */
  private static int firstBlackColumn(byte[] bytes) {
    int rowBytes = 48;
    int min = Integer.MAX_VALUE;
    for (int i = 3 + 8; i < bytes.length; i++) {
      int b = bytes[i] & 0xFF;
      if (b != 0) {
        int col = ((i - 11) % rowBytes) * 8 + Integer.numberOfLeadingZeros(b) - 24;
        min = Math.min(min, col);
      }
    }
    return min;
  }

  @Test
  @DisplayName("an ASCII row pads cells to their widths")
  void shouldPadCells_whenRowIsAscii() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.row(Column.left(16, "Cement 50kg"), Column.right(4, "2"), Column.right(12, "1,300.00"));
    assertThat(ascii(w.build())).isEqualTo("Cement 50kg        2    1,300.00\n");
  }

  @Test
  @DisplayName("centre cells put the extra space on the right")
  void shouldCentre_whenCellIsCentred() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.row(Column.center(6, "ab"), Column.center(5, "c"), Column.left(1, "|"));
    assertThat(ascii(w.build())).isEqualTo("  ab    c  |\n");
  }

  @Test
  @DisplayName("a long cell wraps inside its width and the others stay on the first line")
  void shouldWrapCell_whenTextIsLongerThanCell() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.row(Column.left(10, "Steel wire rope"), Column.right(6, "5"));
    assertThat(ascii(w.build())).isEqualTo("Steel wire     5\nrope\n");
  }

  @Test
  @DisplayName("trailing spaces are not printed")
  void shouldTrimTrailingSpaces_whenLastCellIsShort() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.row(Column.left(10, "a"), Column.left(10, ""));
    assertThat(ascii(w.build())).isEqualTo("a\n");
  }

  @Test
  @DisplayName("a row with one Devanagari cell is one raster as wide as its columns")
  void shouldRasteriseWholeRow_whenOneCellIsDevanagari() {
    ReceiptWriter w = ReceiptWriter.of(rasteriser(PaperWidth.MM58));
    w.row(Column.left(20, "सीमेंट 50 किलो"), Column.right(4, "2"), Column.right(8, "1,300.00"));
    byte[] bytes = w.build();
    // 32 columns * 12 dots = 384 dots = 48 bytes
    assertThat(Hex.text(java.util.Arrays.copyOf(bytes, 6))).isEqualTo("1D 76 30 00 30 00");
  }

  @Test
  @DisplayName("a narrower raster row is as wide as its columns only")
  void shouldUseColumnWidth_whenRasterRowIsNarrow() {
    ReceiptWriter w = ReceiptWriter.of(rasteriser(PaperWidth.MM58));
    w.row(Column.left(10, "क"), Column.right(6, "2"));
    assertThat(Hex.text(java.util.Arrays.copyOf(w.build(), 6))).isEqualTo("1D 76 30 00 18 00");
  }

  @Test
  @DisplayName("rows and rules start at the left edge and restore the alignment")
  void shouldUseLeftEdge_whenAlignedRight() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.align(Alignment.RIGHT).rule('-').row(Column.left(2, "a"));
    String hex = Hex.text(w.build());
    assertThat(hex).startsWith("1B 61 02 1B 61 00 2D");
    assertThat(hex).contains("0A 1B 61 02 1B 61 00 61 0A 1B 61 02");
    assertThat(hex).endsWith("1B 61 02");
  }

  @Test
  @DisplayName("a rule fills the paper columns")
  void shouldFillColumns_whenRule() {
    assertThat(ascii(ReceiptWriter.asciiOnly(PaperWidth.MM58).rule('=').build()))
        .isEqualTo("=".repeat(32) + "\n");
    assertThat(ascii(ReceiptWriter.asciiOnly(PaperWidth.MM80).rule('-').build()))
        .isEqualTo("-".repeat(48) + "\n");
  }

  @Test
  @DisplayName("columns wider than the paper are refused")
  void shouldRefuse_whenColumnsExceedPaper() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    assertThatThrownBy(() -> w.row(Column.left(20, "a"), Column.left(13, "b")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> w.row()).isInstanceOf(IllegalArgumentException.class);
    assertThat(w.row(Column.left(20, "a"), Column.left(12, "b")).build()).isNotEmpty();
  }

  @Test
  @DisplayName("commands() appends to the same stream")
  void shouldShareStream_whenCommandsUsed() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.commands().initialize();
    w.line("x");
    assertThat(Hex.text(w.build())).isEqualTo("1B 40 78 0A");
  }

  @Test
  @DisplayName("Column factories set the alignment")
  void shouldSetAlignment_whenFactoryUsed() {
    assertThat(Column.left(1, "a").alignment()).isEqualTo(Alignment.LEFT);
    assertThat(Column.right(1, "a").alignment()).isEqualTo(Alignment.RIGHT);
    assertThat(Column.center(1, "a").alignment()).isEqualTo(Alignment.CENTER);
    assertThatThrownBy(() -> Column.left(0, "a")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Column(1, null, "a")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Column(1, Alignment.LEFT, null))
        .isInstanceOf(NullPointerException.class);
  }
}
