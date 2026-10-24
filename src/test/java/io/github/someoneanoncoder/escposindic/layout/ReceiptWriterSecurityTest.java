/*
 * File:      ReceiptWriterSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves receipt text cannot inject printer commands through the layout.
 * Owns:      Control characters in lines and cells never reach the byte stream; no rasteriser means refusal.
 * Uses:      ReceiptWriter.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.raster.TextRasteriser;
import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SecurityTest
class ReceiptWriterSecurityTest {

  private static final String INJECTION = "Ramesh\u001Bp\u0000\u0019\u0019";

  @Test
  @DisplayName("an ASCII-only writer refuses a line with a control character")
  void shouldRefuse_whenControlCharacterInAsciiOnlyWriter() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    assertThatThrownBy(() -> w.line(INJECTION)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> w.row(Column.left(10, INJECTION)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(w.build()).isEmpty();
  }

  @Test
  @DisplayName("with a rasteriser a control character is still refused, never sent natively")
  void shouldRefuse_whenControlCharacterAndRasteriserPresent() {
    TextRasteriser raster =
        TextRasteriser.of(TestFonts.latin(22f), PaperWidth.MM58)
            .withFallback(TestFonts.devanagari(24f));
    ReceiptWriter w = ReceiptWriter.of(raster);
    assertThatThrownBy(() -> w.line(INJECTION)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> w.row(Column.left(10, "क"), Column.left(10, INJECTION)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(w.build()).isEmpty();
  }

  @Test
  @DisplayName("non-ASCII text without a rasteriser is refused")
  void shouldRefuse_whenNoRasteriserForDevanagari() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    assertThatThrownBy(() -> w.line("धन्यवाद")).hasMessageContaining("rasteriser");
    assertThatThrownBy(() -> w.row(Column.left(10, "₹5"))).hasMessageContaining("rasteriser");
  }

  @Test
  @DisplayName("a rule character must be printable ASCII")
  void shouldRefuse_whenRuleCharacterIsControl() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    assertThatThrownBy(() -> w.rule('\u001B')).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> w.rule('é')).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("huge ASCII text wraps without growing a line past the paper")
  void shouldBoundLines_whenTextIsHuge() {
    ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
    w.line("x".repeat(100_000));
    String printed = new String(w.build(), java.nio.charset.StandardCharsets.US_ASCII);
    assertThat(printed.split("\n")).allMatch(l -> l.length() <= 32);
  }
}
