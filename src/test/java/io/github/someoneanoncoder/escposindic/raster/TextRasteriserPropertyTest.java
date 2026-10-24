/*
 * File:      TextRasteriserPropertyTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks the rasteriser never fails or misreports its size for valid Devanagari text.
 * Owns:      Width equals the request, ink exists, height stays in bounds.
 * Uses:      TextRasteriser.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic.raster;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.testing.PropertyTest;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

@PropertyTest
class TextRasteriserPropertyTest {

  private static final TextRasteriser R =
      TextRasteriser.of(TestFonts.devanagari(24f), PaperWidth.MM80);

  @Property(tries = 60)
  void shouldKeepWidthAndDrawInk_whenTextIsDevanagari(
      @ForAll("devanagari") @StringLength(min = 1, max = 120) String text,
      @ForAll Alignment alignment,
      @ForAll @IntRange(min = 48, max = 576) int width) {
    BitImage img = R.rasterise(text, width, alignment);
    assertThat(img.width()).isEqualTo(width);
    assertThat(img.height()).isBetween(1, BitImage.MAX_HEIGHT);
    assertThat(img.toBytes()).isNotEqualTo(new byte[img.toBytes().length]);
  }

  @Provide
  Arbitrary<String> devanagari() {
    Arbitrary<Character> letters = Arbitraries.chars().range('क', 'ह');
    Arbitrary<Character> signs = Arbitraries.chars().range('ा', 'ौ');
    return Arbitraries.frequencyOf(
            net.jqwik.api.Tuple.of(6, letters),
            net.jqwik.api.Tuple.of(2, signs),
            net.jqwik.api.Tuple.of(1, Arbitraries.just(' ')))
        .list()
        .ofMinSize(1)
        .ofMaxSize(120)
        .map(
            l -> {
              StringBuilder sb = new StringBuilder("क");
              l.forEach(sb::append);
              return sb.toString();
            });
  }
}
