/*
 * File:      ReceiptWriter.java
 * Module:    escpos-indic
 * Layer:     layout
 * Purpose:   Writes receipt lines, choosing native text for ASCII and raster images for everything else.
 * Owns:      The ASCII-or-raster decision, wrapping, column rows, rules and alignment state.
 * Uses:      EscPosBuilder, TextRasteriser, BitImage, Column, TextWrap.
 * Used by:   Callers printing receipts.
 * Not here:  Receipt templates, barcodes (L3-04), sending bytes.
 * Reuse:     Reach the plain commands through commands().
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.layout;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.EscPosBuilder;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import io.github.someoneanoncoder.escposindic.internal.TextWrap;
import io.github.someoneanoncoder.escposindic.raster.TextRasteriser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lays out receipt text. A line made only of printable ASCII is sent as native printer text, which
 * is fast and uses the printer's own font. A line with any other character (Devanagari, the rupee
 * sign) is drawn by the {@link TextRasteriser} and sent as a raster image. A row with several
 * columns follows the same rule as a whole: one non-ASCII cell makes the entire row a raster so the
 * columns stay aligned.
 *
 * <p>Layout assumes the printer's normal Font A and normal character size. Alignment set with
 * {@link #align(Alignment)} applies to lines; rows and rules always start at the left edge.
 * Emphasis commands do not affect raster text.
 *
 * <p>Instances are mutable and not thread-safe.
 */
public final class ReceiptWriter {

  private final EscPosBuilder commands = EscPosBuilder.create();
  private final PaperWidth paper;
  private final TextRasteriser rasteriser;
  private Alignment alignment = Alignment.LEFT;

  private ReceiptWriter(PaperWidth paper, TextRasteriser rasteriser) {
    this.paper = paper;
    this.rasteriser = rasteriser;
  }

  /**
   * Creates a writer that can print any text.
   *
   * @param rasteriser draws text that is not plain ASCII; its paper width is used, not null
   * @return the writer
   */
  public static ReceiptWriter of(TextRasteriser rasteriser) {
    Objects.requireNonNull(rasteriser, "rasteriser");
    return new ReceiptWriter(rasteriser.paper(), rasteriser);
  }

  /**
   * Creates a writer for plain ASCII text only. Text with other characters is refused.
   *
   * @param paper the paper width, not null
   * @return the writer
   */
  public static ReceiptWriter asciiOnly(PaperWidth paper) {
    return new ReceiptWriter(Objects.requireNonNull(paper, "paper"), null);
  }

  /**
   * Returns the underlying command builder for commands such as initialise, bold, feed, cut and
   * drawer kick. Text and images written through it bypass the layout.
   *
   * @return the builder this writer appends to
   */
  public EscPosBuilder commands() {
    return commands;
  }

  /**
   * Sets the alignment of the following lines.
   *
   * @param newAlignment the alignment, not null
   * @return this writer
   */
  public ReceiptWriter align(Alignment newAlignment) {
    alignment = Objects.requireNonNull(newAlignment, "newAlignment");
    commands.align(newAlignment);
    return this;
  }

  /**
   * Prints text, wrapped to the paper width. ASCII text is native, anything else is raster.
   *
   * @param text the text; line feeds start new lines; not null
   * @return this writer
   * @throws IllegalArgumentException if the text is not ASCII and the writer has no rasteriser, or
   *     the rasteriser refuses it
   */
  public ReceiptWriter line(String text) {
    Objects.requireNonNull(text, "text");
    if (isPlain(text)) {
      for (String wrapped : TextWrap.lines(text, paper.columns())) {
        commands.line(wrapped);
      }
    } else {
      commands.image(requireRasteriser().rasterise(text, alignment));
    }
    return this;
  }

  /**
   * Prints a row of cells. The widths must add up to at most the paper's columns.
   *
   * @param cells at least one cell
   * @return this writer
   * @throws IllegalArgumentException if there are no cells, the widths exceed the paper, or a cell
   *     needs a rasteriser the writer does not have
   */
  public ReceiptWriter row(Column... cells) {
    if (cells.length == 0) {
      throw new IllegalArgumentException("at least one column is required");
    }
    int total = 0;
    boolean allPlain = true;
    for (Column cell : cells) {
      total += cell.widthColumns();
      allPlain &= isPlain(cell.text());
    }
    boolean plain = allPlain;
    if (total > paper.columns()) {
      throw new IllegalArgumentException(
          "columns need " + total + " but the paper has " + paper.columns());
    }
    withLeftEdge(
        () -> {
          if (plain) {
            nativeRow(cells);
          } else {
            commands.image(rasterRow(cells));
          }
        });
    return this;
  }

  /**
   * Prints an image such as a QR code or barcode, placed on the paper by the current alignment.
   *
   * @param image the image, not null, at most as wide as the paper
   * @return this writer
   * @throws IllegalArgumentException if the image is wider than the paper
   */
  public ReceiptWriter image(BitImage image) {
    Objects.requireNonNull(image, "image");
    commands.image(image.padded(paper.dots(), alignment));
    return this;
  }

  /**
   * Prints a line made of one repeated character across the full width, for example a dashed rule.
   *
   * @param fill a printable ASCII character
   * @return this writer
   * @throws IllegalArgumentException if the character is not printable ASCII
   */
  public ReceiptWriter rule(char fill) {
    if (!EscPosBuilder.isPrintableAscii(fill)) {
      throw new IllegalArgumentException("rule character must be printable ASCII");
    }
    withLeftEdge(() -> commands.line(String.valueOf(fill).repeat(paper.columns())));
    return this;
  }

  /**
   * Returns the bytes written so far.
   *
   * @return a copy of the stream
   */
  public byte[] build() {
    return commands.build();
  }

  private void nativeRow(Column[] cells) {
    List<List<String>> columns = new ArrayList<>();
    int rows = 1;
    for (Column cell : cells) {
      List<String> lines = TextWrap.lines(cell.text(), cell.widthColumns());
      columns.add(lines);
      rows = Math.max(rows, lines.size());
    }
    for (int r = 0; r < rows; r++) {
      StringBuilder line = new StringBuilder();
      for (int c = 0; c < cells.length; c++) {
        String part = r < columns.get(c).size() ? columns.get(c).get(r) : "";
        line.append(pad(part, cells[c].widthColumns(), cells[c].alignment()));
      }
      commands.line(line.toString().stripTrailing());
    }
  }

  private BitImage rasterRow(Column[] cells) {
    TextRasteriser drawer = requireRasteriser();
    List<BitImage> parts = new ArrayList<>();
    for (Column cell : cells) {
      int dots = cell.widthColumns() * PaperWidth.DOTS_PER_COLUMN;
      String text = cell.text().isEmpty() ? " " : cell.text();
      parts.add(drawer.rasterise(text, dots, cell.alignment()));
    }
    return BitImage.sideBySide(parts);
  }

  private void withLeftEdge(Runnable action) {
    if (alignment != Alignment.LEFT) {
      commands.align(Alignment.LEFT);
    }
    action.run();
    if (alignment != Alignment.LEFT) {
      commands.align(alignment);
    }
  }

  private TextRasteriser requireRasteriser() {
    if (rasteriser == null) {
      throw new IllegalArgumentException(
          "text is not plain ASCII and this writer has no rasteriser");
    }
    return rasteriser;
  }

  private static boolean isPlain(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c != '\n' && !EscPosBuilder.isPrintableAscii(c)) {
        return false;
      }
    }
    return true;
  }

  private static String pad(String text, int width, Alignment how) {
    int slack = width - text.length();
    return switch (how) {
      case LEFT -> text + " ".repeat(slack);
      case RIGHT -> " ".repeat(slack) + text;
      case CENTER -> " ".repeat(slack / 2) + text + " ".repeat(slack - slack / 2);
    };
  }
}
