/*
 * File:      Column.java
 * Module:    escpos-indic
 * Layer:     layout
 * Purpose:   One cell of a table row: text, width in character columns and alignment.
 * Owns:      Cell definition and its validation.
 * Uses:      Alignment.
 * Used by:   ReceiptWriter.row.
 * Not here:  Drawing (ReceiptWriter does that).
 * Reuse:     Immutable record.
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.layout;

import io.github.someoneanoncoder.escposindic.Alignment;
import java.util.Objects;

/**
 * A cell of a row printed with {@link ReceiptWriter#row(Column...)}.
 *
 * <p>The width is in Font A character columns ({@code 12} dots each), so the same row definition
 * works for native text and for raster text. The cell does not add a gap to its neighbour: leave a
 * spare column in the width where the text could touch the next cell.
 *
 * @param widthColumns width in character columns, at least 1
 * @param alignment where the text sits inside the cell, not null
 * @param text the text; may be empty; line feeds start a new line inside the cell
 */
public record Column(int widthColumns, Alignment alignment, String text) {

  /** Validates the cell. */
  public Column {
    if (widthColumns < 1) {
      throw new IllegalArgumentException("widthColumns must be at least 1");
    }
    Objects.requireNonNull(alignment, "alignment");
    Objects.requireNonNull(text, "text");
  }

  /**
   * Creates a left-aligned cell, for item names.
   *
   * @param widthColumns width in character columns
   * @param text the text
   * @return the cell
   */
  public static Column left(int widthColumns, String text) {
    return new Column(widthColumns, Alignment.LEFT, text);
  }

  /**
   * Creates a right-aligned cell, for quantities and amounts.
   *
   * @param widthColumns width in character columns
   * @param text the text
   * @return the cell
   */
  public static Column right(int widthColumns, String text) {
    return new Column(widthColumns, Alignment.RIGHT, text);
  }

  /**
   * Creates a centred cell.
   *
   * @param widthColumns width in character columns
   * @param text the text
   * @return the cell
   */
  public static Column center(int widthColumns, String text) {
    return new Column(widthColumns, Alignment.CENTER, text);
  }
}
