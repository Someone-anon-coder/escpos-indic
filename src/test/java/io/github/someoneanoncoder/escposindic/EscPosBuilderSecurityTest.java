/*
 * File:      EscPosBuilderSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves untrusted text cannot inject printer commands.
 * Owns:      Refusal of control characters, non-ASCII and line breaks in native text.
 * Uses:      EscPosBuilder.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SecurityTest
class EscPosBuilderSecurityTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\u001B@", // ESC @ would reset the printer
        "\u001Bp\u0000\u0019\u0019", // ESC p would open the cash drawer
        "\u001DVA\u0000", // GS V would cut the paper
        "\u001Dv0\u0000ÿÿÿÿ", // GS v 0 would start a huge image
        "line\nbreak",
        "carriage\rreturn",
        "tab\there",
        "nul\u0000byte",
        "delete\u007F",
        "latin1 é",
        "rupee ₹",
        "हिन्दी"
      })
  @DisplayName("native text refuses anything but printable ASCII")
  void shouldRefuse_whenTextHasControlOrNonAscii(String text) {
    EscPosBuilder b = EscPosBuilder.create();
    assertThatThrownBy(() -> b.text(text)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.line(text)).isInstanceOf(IllegalArgumentException.class);
    assertThat(b.length()).isZero();
  }

  @Test
  @DisplayName("the error message does not echo the refused text")
  void shouldNotEchoText_whenRefusing() {
    assertThatThrownBy(() -> EscPosBuilder.create().text("Ramesh \u001B@"))
        .hasMessageNotContaining("Ramesh");
  }

  @Test
  @DisplayName("a refused text leaves earlier bytes intact and adds nothing")
  void shouldKeepEarlierBytes_whenLaterTextRefused() {
    EscPosBuilder b = EscPosBuilder.create().initialize();
    assertThatThrownBy(() -> b.text("ok\u001B")).isInstanceOf(IllegalArgumentException.class);
    assertThat(b.length()).isEqualTo(2);
  }
}
