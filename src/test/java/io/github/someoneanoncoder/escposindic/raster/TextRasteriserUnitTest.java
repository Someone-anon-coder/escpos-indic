/*
 * File:      TextRasteriserUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks Devanagari rendering, wrapping, alignment and refusals with the test font.
 * Owns:      Image size, ink placement, wrapping, shaping, threshold.
 * Uses:      TextRasteriser, TestFonts.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic.raster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.testing.TestFonts;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class TextRasteriserUnitTest {

  private static final String HINDI = "हिन्दी";
  private final TextRasteriser r58 = TextRasteriser.of(TestFonts.devanagari(24f), PaperWidth.MM58);

  static int blackCount(BitImage img) {
    int n = 0;
    for (int y = 0; y < img.height(); y++) {
      for (int x = 0; x < img.width(); x++) {
        n += img.isBlack(x, y) ? 1 : 0;
      }
    }
    return n;
  }

  private static int leftmostInk(BitImage img) {
    for (int x = 0; x < img.width(); x++) {
      for (int y = 0; y < img.height(); y++) {
        if (img.isBlack(x, y)) {
          return x;
        }
      }
    }
    return -1;
  }

  private static int rightmostInk(BitImage img) {
    for (int x = img.width() - 1; x >= 0; x--) {
      for (int y = 0; y < img.height(); y++) {
        if (img.isBlack(x, y)) {
          return x;
        }
      }
    }
    return -1;
  }

  @Test
  @DisplayName("the image is exactly as wide as the paper at both widths")
  void shouldMatchPaperWidth_whenRasterising() {
    assertThat(r58.rasterise(HINDI, Alignment.LEFT).width()).isEqualTo(384);
    TextRasteriser r80 = TextRasteriser.of(TestFonts.devanagari(24f), PaperWidth.MM80);
    assertThat(r80.rasterise(HINDI, Alignment.LEFT).width()).isEqualTo(576);
    assertThat(r80.paper()).isEqualTo(PaperWidth.MM80);
  }

  @Test
  @DisplayName("Hindi text leaves ink and a line is about one font size tall")
  void shouldDrawInk_whenTextIsHindi() {
    BitImage img = r58.rasterise(HINDI, Alignment.LEFT);
    assertThat(blackCount(img)).isGreaterThan(100);
    assertThat(img.height()).isBetween(24, 60);
  }

  @Test
  @DisplayName("left, centre and right put the ink where they say")
  void shouldPlaceInk_whenAligned() {
    BitImage left = r58.rasterise(HINDI, Alignment.LEFT);
    BitImage center = r58.rasterise(HINDI, Alignment.CENTER);
    BitImage right = r58.rasterise(HINDI, Alignment.RIGHT);
    assertThat(leftmostInk(left)).isLessThan(4);
    assertThat(rightmostInk(right)).isGreaterThan(380);
    int centreMid = (leftmostInk(center) + rightmostInk(center)) / 2;
    assertThat(centreMid).isBetween(188, 196);
    assertThat(blackCount(left))
        .isCloseTo(blackCount(right), org.assertj.core.data.Percentage.withPercentage(3));
  }

  @Test
  @DisplayName("long text wraps at spaces and every line stays inside the width")
  void shouldWrap_whenTextIsLongerThanLine() {
    String sentence = "यह एक लंबा वाक्य है जो एक पंक्ति में नहीं समाएगा और अगली पंक्ति पर जाएगा";
    BitImage wrapped = r58.rasterise(sentence, Alignment.LEFT);
    BitImage single = r58.rasterise(HINDI, Alignment.LEFT);
    assertThat(wrapped.height()).isGreaterThan(single.height() * 3 / 2);
    assertThat(rightmostInk(wrapped)).isLessThan(384);
  }

  @Test
  @DisplayName("a line feed starts a new paragraph, an empty paragraph keeps its height")
  void shouldBreakLines_whenTextHasLineFeed() {
    int one = r58.rasterise("क", Alignment.LEFT).height();
    assertThat(r58.rasterise("क\nख", Alignment.LEFT).height()).isBetween(2 * one - 2, 2 * one + 2);
    assertThat(r58.rasterise("क\n\nख", Alignment.LEFT).height())
        .isBetween(3 * one - 3, 3 * one + 3);
  }

  @Test
  @DisplayName("conjuncts are shaped: क्ष is narrower than its three code points drawn apart")
  void shouldShapeConjunct_whenMeasuring() {
    int shaped = r58.measure("क्ष");
    int apart = r58.measure("क") + r58.measure("्") + r58.measure("ष");
    assertThat(shaped).isLessThan(apart);
    assertThat(r58.measure("ि")).isPositive();
  }

  @Test
  @DisplayName("a narrower width gives a taller image and exactly that width")
  void shouldRespectCellWidth_whenRasterisingIntoCell() {
    String text = "नमस्ते दुकानदार जी";
    BitImage wide = r58.rasterise(text, 384, Alignment.LEFT);
    BitImage narrow = r58.rasterise(text, 96, Alignment.LEFT);
    assertThat(narrow.width()).isEqualTo(96);
    assertThat(narrow.height()).isGreaterThan(wide.height());
  }

  @Test
  @DisplayName("a higher threshold turns more grey dots black")
  void shouldDarken_whenThresholdRaised() {
    int low = blackCount(r58.withThreshold(60).rasterise(HINDI, Alignment.LEFT));
    int high = blackCount(r58.withThreshold(240).rasterise(HINDI, Alignment.LEFT));
    assertThat(high).isGreaterThan(low);
    int normal = blackCount(r58.rasterise(HINDI, Alignment.LEFT));
    assertThat(normal).isBetween(low, high);
    assertThat(TextRasteriser.DEFAULT_THRESHOLD).isEqualTo(160);
  }

  @Test
  @DisplayName("digits and ASCII punctuation in the Devanagari font render")
  void shouldDrawDigits_whenTextHasAsciiDigits() {
    assertThat(blackCount(r58.rasterise("1,250.00", Alignment.RIGHT))).isPositive();
  }

  @Test
  @DisplayName("a bigger font gives a taller image")
  void shouldScale_whenFontIsBigger() {
    TextRasteriser big = TextRasteriser.of(TestFonts.devanagari(48f), PaperWidth.MM58);
    assertThat(big.rasterise(HINDI, Alignment.LEFT).height())
        .isGreaterThan(r58.rasterise(HINDI, Alignment.LEFT).height() * 3 / 2);
  }

  @Test
  @DisplayName("measure refuses multi-line text")
  void shouldRefuse_whenMeasuringTwoLines() {
    assertThatThrownBy(() -> r58.measure("क\nख")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("a fallback font draws characters the first font lacks")
  void shouldUseFallback_whenFirstFontLacksCharacter() {
    TextRasteriser mixed =
        TextRasteriser.of(TestFonts.devanagari(24f), PaperWidth.MM58)
            .withFallback(TestFonts.latin(22f));
    assertThat(blackCount(mixed.rasterise("Total कुल", Alignment.LEFT))).isPositive();
    assertThat(mixed.measure("Total")).isGreaterThan(40);
    assertThatThrownBy(() -> r58.rasterise("Total", Alignment.LEFT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("fonts are tried in the order they were added")
  void shouldPreferFirstFont_whenBothCanDisplay() {
    TextRasteriser latinFirst =
        TextRasteriser.of(TestFonts.latin(22f), PaperWidth.MM58)
            .withFallback(TestFonts.devanagari(24f));
    TextRasteriser devaFirst =
        TextRasteriser.of(TestFonts.devanagari(24f), PaperWidth.MM58)
            .withFallback(TestFonts.latin(22f));
    assertThat(latinFirst.measure("1250")).isNotEqualTo(devaFirst.measure("1250"));
  }

  @Test
  @DisplayName("zero width joiner is accepted and follows the previous font")
  void shouldAcceptJoiner_whenTextHasZeroWidthJoiner() {
    assertThat(blackCount(r58.rasterise("क्\u200Dष", Alignment.LEFT))).isPositive();
    assertThat(blackCount(r58.rasterise("क्\u200Cष", Alignment.LEFT))).isPositive();
  }
}
