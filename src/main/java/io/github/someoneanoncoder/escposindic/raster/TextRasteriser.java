/*
 * File:      TextRasteriser.java
 * Module:    escpos-indic
 * Layer:     raster
 * Purpose:   Renders shaped text in any font (Devanagari included) to a 1-bit image.
 * Owns:      Java2D text layout, word wrapping, alignment inside a width, thresholding, text limits.
 * Uses:      java.awt (Font, TextLayout, LineBreakMeasurer), BitImage, PaperWidth.
 * Used by:   Callers printing Hindi/Marathi text; the line layout (L3-03).
 * Not here:  Font files (the caller supplies a Font), ESC/POS bytes (EscPosBuilder.image).
 * Reuse:     One instance per font and size; instances are immutable and thread-safe.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic.raster;

import io.github.someoneanoncoder.escposindic.Alignment;
import io.github.someoneanoncoder.escposindic.BitImage;
import io.github.someoneanoncoder.escposindic.PaperWidth;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Draws text with Java2D, which shapes complex scripts such as Devanagari (conjuncts, vowel signs
 * and reordering), wraps it to a width and converts it to a {@link BitImage} ready for {@link
 * io.github.someoneanoncoder.escposindic.EscPosBuilder#image(BitImage)}.
 *
 * <p>The library bundles no font. The caller supplies a {@link Font} at the wanted pixel size, for
 * example {@code Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(24f)}; one pixel is one
 * printer dot. About 24 dots gives readable text on 203 dpi receipt printers. The font must contain
 * every character of the text, otherwise {@link #rasterise(String, int, Alignment)} refuses it
 * instead of printing empty boxes.
 *
 * <p>Text is limited to {@value #MAX_CHARACTERS} characters and the image to {@value
 * BitImage#MAX_HEIGHT} rows. Only the line feed is accepted as a control character and it starts a
 * new paragraph.
 */
public final class TextRasteriser {

  /** Longest text accepted in one call. */
  public static final int MAX_CHARACTERS = 2000;

  /** Brightness threshold used unless another is given. */
  public static final int DEFAULT_THRESHOLD = 160;

  private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

  private final Font font;
  private final PaperWidth paper;
  private final int threshold;

  private TextRasteriser(Font font, PaperWidth paper, int threshold) {
    this.font = font;
    this.paper = paper;
    this.threshold = threshold;
  }

  /**
   * Creates a rasteriser for a font and paper width using {@link #DEFAULT_THRESHOLD}.
   *
   * @param font the font at its final size, not null
   * @param paper the paper width, not null
   * @return the rasteriser
   */
  public static TextRasteriser of(Font font, PaperWidth paper) {
    return new TextRasteriser(
        Objects.requireNonNull(font, "font"),
        Objects.requireNonNull(paper, "paper"),
        DEFAULT_THRESHOLD);
  }

  /**
   * Returns a copy that converts to black and white at another brightness threshold. A higher value
   * makes thin strokes darker.
   *
   * @param newThreshold 1 to 255; anti-aliased dots darker than this become black
   * @return the new rasteriser
   * @throws IllegalArgumentException if the threshold is out of range
   */
  public TextRasteriser withThreshold(int newThreshold) {
    if (newThreshold < 1 || newThreshold > 255) {
      throw new IllegalArgumentException("threshold must be 1 to 255 but was " + newThreshold);
    }
    return new TextRasteriser(font, paper, newThreshold);
  }

  /**
   * Returns the paper width this rasteriser targets.
   *
   * @return the paper width
   */
  public PaperWidth paper() {
    return paper;
  }

  /**
   * Renders text across the full paper width, wrapped at word boundaries.
   *
   * @param text the text, not null, up to {@value #MAX_CHARACTERS} characters
   * @param alignment where each line sits inside the width, not null
   * @return an image exactly {@link PaperWidth#dots()} wide
   * @throws IllegalArgumentException if the text is empty, too long, holds unsupported control
   *     characters or characters the font cannot display
   */
  public BitImage rasterise(String text, Alignment alignment) {
    return rasterise(text, paper.dots(), alignment);
  }

  /**
   * Renders text into a given width, wrapped at word boundaries. Used for table cells.
   *
   * @param text the text, not null, up to {@value #MAX_CHARACTERS} characters
   * @param widthDots width of the image in dots, 1 to {@value BitImage#MAX_WIDTH}
   * @param alignment where each line sits inside the width, not null
   * @return an image exactly {@code widthDots} wide
   * @throws IllegalArgumentException if the text is empty, too long, holds unsupported control
   *     characters or characters the font cannot display, or the width is out of range
   */
  public BitImage rasterise(String text, int widthDots, Alignment alignment) {
    Objects.requireNonNull(alignment, "alignment");
    if (widthDots < 1 || widthDots > BitImage.MAX_WIDTH) {
      throw new IllegalArgumentException("widthDots must be 1 to " + BitImage.MAX_WIDTH);
    }
    checkText(text);
    List<TextLayout> lines = layoutLines(text, widthDots);
    return BitImage.fromImage(draw(lines, widthDots, alignment), threshold);
  }

  /**
   * Measures the width one line of text needs without wrapping.
   *
   * @param text the text, not null, no line feed
   * @return the advance width in dots, rounded up
   * @throws IllegalArgumentException as for {@link #rasterise(String, Alignment)}, or if the text
   *     holds a line feed
   */
  public int measure(String text) {
    checkText(text);
    if (text.indexOf('\n') >= 0) {
      throw new IllegalArgumentException("measure takes a single line");
    }
    return (int) Math.ceil(new TextLayout(text, font, CONTEXT).getAdvance());
  }

  private void checkText(String text) {
    Objects.requireNonNull(text, "text");
    if (text.isEmpty() || text.length() > MAX_CHARACTERS) {
      throw new IllegalArgumentException("text must be 1 to " + MAX_CHARACTERS + " characters");
    }
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (Character.isISOControl(c) && c != '\n') {
        throw new IllegalArgumentException("control character at index " + i);
      }
    }
    int bad = font.canDisplayUpTo(text);
    if (bad >= 0) {
      throw new IllegalArgumentException("the font cannot display the character at index " + bad);
    }
  }

  private List<TextLayout> layoutLines(String text, int widthDots) {
    List<TextLayout> lines = new ArrayList<>();
    for (String paragraph : text.split("\n", -1)) {
      if (paragraph.isEmpty()) {
        lines.add(new TextLayout(" ", font, CONTEXT));
        continue;
      }
      AttributedString attributed = new AttributedString(paragraph);
      attributed.addAttribute(TextAttribute.FONT, font);
      AttributedCharacterIterator iterator = attributed.getIterator();
      LineBreakMeasurer measurer = new LineBreakMeasurer(iterator, CONTEXT);
      while (measurer.getPosition() < iterator.getEndIndex()) {
        lines.add(measurer.nextLayout(widthDots));
      }
    }
    return lines;
  }

  private BufferedImage draw(List<TextLayout> lines, int widthDots, Alignment alignment) {
    double total = 0;
    for (TextLayout line : lines) {
      total += lineHeight(line);
    }
    int height = (int) Math.ceil(total);
    if (height > BitImage.MAX_HEIGHT) {
      throw new IllegalArgumentException("text is too tall");
    }
    BufferedImage image = new BufferedImage(widthDots, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, widthDots, height);
      g.setColor(Color.BLACK);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      double baseline = 0;
      for (TextLayout line : lines) {
        baseline += line.getAscent();
        line.draw(g, (float) startX(line, widthDots, alignment), (float) baseline);
        baseline += line.getDescent() + line.getLeading();
      }
    } finally {
      g.dispose();
    }
    return image;
  }

  private static double lineHeight(TextLayout line) {
    return line.getAscent() + line.getDescent() + line.getLeading();
  }

  private static double startX(TextLayout line, int widthDots, Alignment alignment) {
    double slack = Math.max(0, widthDots - line.getAdvance());
    return switch (alignment) {
      case LEFT -> 0;
      case CENTER -> slack / 2;
      case RIGHT -> slack;
    };
  }
}
