/*
 * File:      Hex.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Hex helpers for comparing byte streams in tests.
 * Owns:      Parsing and formatting of space-separated hex bytes.
 * Uses:      Nothing.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic.testing;

import java.util.HexFormat;

/** Converts between byte arrays and readable hex text. */
public final class Hex {

  private static final HexFormat FORMAT = HexFormat.ofDelimiter(" ").withUpperCase();

  private Hex() {}

  /**
   * Parses space-separated hex bytes.
   *
   * @param text for example {@code "1B 40"}
   * @return the bytes
   */
  public static byte[] bytes(String text) {
    return text.isBlank() ? new byte[0] : FORMAT.parseHex(text.trim());
  }

  /**
   * Formats bytes as upper-case hex separated by spaces.
   *
   * @param data the bytes
   * @return for example {@code "1B 40"}
   */
  public static String text(byte[] data) {
    return FORMAT.formatHex(data);
  }
}
