/*
 * File:      PaperWidth.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   Paper roll widths with their printable dot width and character columns.
 * Owns:      The 58 mm (384 dot) and 80 mm (576 dot) widths.
 * Uses:      Nothing.
 * Used by:   EscPosBuilder callers, the rasteriser and the line layout.
 * Not here:  Printer-specific quirks (margins, other widths).
 * Reuse:     Extend by adding a constant only with a vendor-documented width.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

/**
 * Thermal paper widths the library lays out for.
 *
 * <p>Dot widths are the usual printable widths of 203 dpi receipt printers: 384 dots on 58 mm paper
 * and 576 dots on 80 mm paper. Character columns are for the printer's default Font A, which is 12
 * dots wide, so one column is always {@value #DOTS_PER_COLUMN} dots. These figures are common
 * printer specifications, not part of the ESC/POS command set; see the README under "Not verified".
 */
public enum PaperWidth {
  /** 58 mm paper: 384 dots, 32 columns of Font A. */
  MM58(384),
  /** 80 mm paper: 576 dots, 48 columns of Font A. */
  MM80(576);

  /** Width of one Font A character cell in dots. */
  public static final int DOTS_PER_COLUMN = 12;

  private final int dots;

  PaperWidth(int dots) {
    this.dots = dots;
  }

  /**
   * Returns the printable width in dots.
   *
   * @return 384 for {@link #MM58}, 576 for {@link #MM80}
   */
  public int dots() {
    return dots;
  }

  /**
   * Returns the number of Font A character columns on one line.
   *
   * @return {@link #dots()} divided by {@link #DOTS_PER_COLUMN}
   */
  public int columns() {
    return dots / DOTS_PER_COLUMN;
  }

  /**
   * Returns the number of bytes in one raster row at this width.
   *
   * @return {@link #dots()} divided by 8
   */
  public int bytesPerRow() {
    return dots / 8;
  }
}
