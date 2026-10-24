/*
 * File:      EscPosBuilderGoldenTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Compares every command's bytes with golden files written by hand from the command reference.
 * Owns:      One golden hex file per command.
 * Uses:      EscPosBuilder, GoldenFiles.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-06
 */
package io.github.someoneanoncoder.escposindic;

import io.github.someoneanoncoder.escposindic.testing.GoldenFiles;
import io.github.someoneanoncoder.escposindic.testing.GoldenTest;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@GoldenTest
class EscPosBuilderGoldenTest {

  private static Arguments command(String name, Supplier<EscPosBuilder> build) {
    return Arguments.of(name, build);
  }

  static Stream<Arguments> commands() {
    return Stream.of(
        command("initialize", () -> EscPosBuilder.create().initialize()),
        command("align-left", () -> EscPosBuilder.create().align(Alignment.LEFT)),
        command("align-center", () -> EscPosBuilder.create().align(Alignment.CENTER)),
        command("align-right", () -> EscPosBuilder.create().align(Alignment.RIGHT)),
        command("bold-on", () -> EscPosBuilder.create().bold(true)),
        command("bold-off", () -> EscPosBuilder.create().bold(false)),
        command("underline-off", () -> EscPosBuilder.create().underline(0)),
        command("underline-1dot", () -> EscPosBuilder.create().underline(1)),
        command("underline-2dot", () -> EscPosBuilder.create().underline(2)),
        command("size-1x1", () -> EscPosBuilder.create().size(1, 1)),
        command("size-2x2", () -> EscPosBuilder.create().size(2, 2)),
        command("size-3x5", () -> EscPosBuilder.create().size(3, 5)),
        command("size-8x8", () -> EscPosBuilder.create().size(8, 8)),
        command("feed-lines-3", () -> EscPosBuilder.create().feed(3)),
        command("feed-units-24", () -> EscPosBuilder.create().feedUnits(24)),
        command("line-spacing-30", () -> EscPosBuilder.create().lineSpacing(30)),
        command("line-spacing-default", () -> EscPosBuilder.create().lineSpacingDefault()),
        command("cut-full", () -> EscPosBuilder.create().cut(CutMode.FULL)),
        command("cut-partial", () -> EscPosBuilder.create().cut(CutMode.PARTIAL)),
        command("drawer-pin2-default", () -> EscPosBuilder.create().drawerKick(DrawerPin.PIN_2)),
        command(
            "drawer-pin5-200-400",
            () -> EscPosBuilder.create().drawerKick(DrawerPin.PIN_5, 200, 400)),
        command("text-ascii", () -> EscPosBuilder.create().text("Hello 123")),
        command("line-ascii", () -> EscPosBuilder.create().line("Hello")),
        command("new-line", () -> EscPosBuilder.create().newLine()),
        command(
            "image-8x2",
            () -> EscPosBuilder.create().image(BitImage.of(8, 2, new byte[] {(byte) 0xFF, 0}))),
        command(
            "image-10x1",
            () ->
                EscPosBuilder.create()
                    .image(BitImage.of(10, 1, new byte[] {(byte) 0xAA, (byte) 0xFF}))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("commands")
  @DisplayName("each command equals its hand-written golden bytes")
  void shouldMatchGolden_whenCommandBuilt(String name, Supplier<EscPosBuilder> build) {
    GoldenFiles.assertHex("commands/" + name, build.get().build());
  }
}
