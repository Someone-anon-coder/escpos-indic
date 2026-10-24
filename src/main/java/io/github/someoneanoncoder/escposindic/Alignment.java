/*
 * File:      Alignment.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   Horizontal alignment values for ESC a.
 * Owns:      The three alignment codes.
 * Uses:      Nothing.
 * Used by:   EscPosBuilder.
 * Not here:  Layout decisions.
 * Reuse:     Fixed by the command set.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

/**
 * Justification of the following lines, command {@code ESC a n} (select justification).
 *
 * <p>Source: Epson ESC/POS Command Reference, "ESC a": n = 0 left, 1 centre, 2 right.
 */
public enum Alignment {
  /** Left-justified, {@code n = 0}. */
  LEFT(0),
  /** Centred, {@code n = 1}. */
  CENTER(1),
  /** Right-justified, {@code n = 2}. */
  RIGHT(2);

  private final int code;

  Alignment(int code) {
    this.code = code;
  }

  /**
   * Returns the parameter byte sent after {@code ESC a}.
   *
   * @return 0, 1 or 2
   */
  public int code() {
    return code;
  }
}
