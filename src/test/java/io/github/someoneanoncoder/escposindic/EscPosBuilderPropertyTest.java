/*
 * File:      EscPosBuilderPropertyTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks builder invariants over random inputs.
 * Owns:      Text round trip, size nibbles, refusal of any control character.
 * Uses:      EscPosBuilder.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.PropertyTest;
import java.nio.charset.StandardCharsets;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

@PropertyTest
class EscPosBuilderPropertyTest {

  @Property
  void shouldEmitSameBytes_whenTextIsPrintableAscii(
      @ForAll("printable") @StringLength(max = 200) String text) {
    assertThat(EscPosBuilder.create().text(text).build())
        .isEqualTo(text.getBytes(StandardCharsets.US_ASCII));
    assertThat(EscPosBuilder.create().line(text).length()).isEqualTo(text.length() + 1);
  }

  @Property
  void shouldRefuse_whenAnyCharacterIsNotPrintable(
      @ForAll("printable") @StringLength(max = 50) String head,
      @ForAll("unprintable") char bad,
      @ForAll("printable") @StringLength(max = 50) String tail) {
    assertThatThrownBy(() -> EscPosBuilder.create().text(head + bad + tail))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void shouldPackSizeIntoNibbles_whenMultiplesInRange(
      @ForAll @IntRange(min = 1, max = 8) int width,
      @ForAll @IntRange(min = 1, max = 8) int height) {
    byte[] bytes = EscPosBuilder.create().size(width, height).build();
    assertThat(bytes).hasSize(3);
    int n = bytes[2] & 0xFF;
    assertThat((n >> 4) + 1).isEqualTo(width);
    assertThat((n & 0x0F) + 1).isEqualTo(height);
    assertThat(n & 0x88).isZero();
  }

  @Property
  void shouldKeepTimesWithinOneByte_whenDrawerTimesInRange(
      @ForAll @IntRange(min = 0, max = 510) int on, @ForAll @IntRange(min = 0, max = 510) int off) {
    byte[] bytes = EscPosBuilder.create().drawerKick(DrawerPin.PIN_2, on, off).build();
    assertThat(bytes).hasSize(5);
    assertThat((bytes[3] & 0xFF) * 2).isBetween(on, on + 1);
    assertThat((bytes[4] & 0xFF) * 2).isBetween(off, off + 1);
  }

  @Provide
  Arbitrary<String> printable() {
    return Arbitraries.strings().withCharRange(' ', '~');
  }

  @Provide
  Arbitrary<Character> unprintable() {
    Arbitrary<Character> control = Arbitraries.chars().range('\u0000', '\u001F');
    Arbitrary<Character> high = Arbitraries.chars().range('\u007F', '￿');
    return Combinators.combine(Arbitraries.of(true, false), control, high)
        .as((useControl, c, h) -> useControl ? c : h);
  }
}
