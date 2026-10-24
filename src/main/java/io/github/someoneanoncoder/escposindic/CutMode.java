/*
 * File:      CutMode.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   Paper cut modes for GS V function B.
 * Owns:      Full and partial cut.
 * Uses:      Nothing.
 * Used by:   EscPosBuilder.
 * Not here:  Cutter capability detection.
 * Reuse:     Fixed by the command set.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

/**
 * How the paper is cut, command {@code GS V m n} (select cut mode and cut paper), function B.
 *
 * <p>Source: Epson ESC/POS Command Reference, "GS V": m = 65 feeds the paper to the cutting
 * position and then cuts fully, m = 66 cuts partially. The library always uses these forms because
 * the immediate forms (m = 0 and 1) cut at the print head position and would slice through the last
 * printed line. Printers without a partial cutter usually cut fully; that is printer behaviour.
 */
public enum CutMode {
  /** Full cut after feeding to the cutting position, {@code m = 65}. */
  FULL(65),
  /** Partial cut after feeding to the cutting position, {@code m = 66}. */
  PARTIAL(66);

  private final int code;

  CutMode(int code) {
    this.code = code;
  }

  /**
   * Returns the {@code m} byte sent after {@code GS V}.
   *
   * @return 65 or 66
   */
  public int code() {
    return code;
  }
}
