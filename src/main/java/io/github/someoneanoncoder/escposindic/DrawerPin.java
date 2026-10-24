/*
 * File:      DrawerPin.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   Cash drawer connector pins for ESC p.
 * Owns:      Pin 2 and pin 5 codes.
 * Uses:      Nothing.
 * Used by:   EscPosBuilder.
 * Not here:  Drawer wiring.
 * Reuse:     Fixed by the command set.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

/**
 * Drawer connector pin that receives the pulse, command {@code ESC p m t1 t2} (generate pulse).
 *
 * <p>Source: Epson ESC/POS Command Reference, "ESC p": m = 0 drawer kick-out connector pin 2, m = 1
 * pin 5.
 */
public enum DrawerPin {
  /** Connector pin 2, {@code m = 0}. */
  PIN_2(0),
  /** Connector pin 5, {@code m = 1}. */
  PIN_5(1);

  private final int code;

  DrawerPin(int code) {
    this.code = code;
  }

  /**
   * Returns the {@code m} byte sent after {@code ESC p}.
   *
   * @return 0 or 1
   */
  public int code() {
    return code;
  }
}
