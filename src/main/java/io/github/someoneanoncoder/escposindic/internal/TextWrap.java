/*
 * File:      TextWrap.java
 * Module:    escpos-indic
 * Layer:     internal
 * Purpose:   Word wrapping of plain text to a number of character columns.
 * Owns:      Greedy wrapping at spaces, hard splitting of over-long words, paragraphs by line feed.
 * Uses:      Nothing.
 * Used by:   ReceiptWriter.
 * Not here:  Pixel-based wrapping (TextRasteriser does that).
 * Reuse:     Not exported.
 * Plan:      L3-03
 */
package io.github.someoneanoncoder.escposindic.internal;

import java.util.ArrayList;
import java.util.List;

/** Wraps text for native (character cell) printing. */
public final class TextWrap {

  private TextWrap() {}

  /**
   * Splits text into lines of at most {@code width} characters. Lines break at spaces; a word
   * longer than the width is cut. A line feed always starts a new line and an empty paragraph gives
   * one empty line.
   *
   * @param text the text, not null
   * @param width characters per line, at least 1
   * @return one or more lines, none longer than {@code width}
   * @throws IllegalArgumentException if {@code width} is less than 1
   */
  public static List<String> lines(String text, int width) {
    if (width < 1) {
      throw new IllegalArgumentException("width must be at least 1");
    }
    List<String> result = new ArrayList<>();
    for (String paragraph : text.split("\n", -1)) {
      wrapParagraph(paragraph, width, result);
    }
    return result;
  }

  private static void wrapParagraph(String paragraph, int width, List<String> result) {
    StringBuilder current = new StringBuilder();
    for (String word : paragraph.split(" ", -1)) {
      if (word.isEmpty()) {
        continue;
      }
      String rest = word;
      while (rest.length() > width) {
        if (current.length() > 0) {
          result.add(current.toString());
          current.setLength(0);
        }
        result.add(rest.substring(0, width));
        rest = rest.substring(width);
      }
      if (current.length() > 0 && current.length() + 1 + rest.length() > width) {
        result.add(current.toString());
        current.setLength(0);
      }
      if (current.length() > 0) {
        current.append(' ');
      }
      current.append(rest);
    }
    result.add(current.toString());
  }
}
