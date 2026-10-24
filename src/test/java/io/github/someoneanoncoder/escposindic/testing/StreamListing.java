/*
 * File:      StreamListing.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Turns an ESC/POS byte stream into readable text so a golden diff shows what changed.
 * Owns:      Decoding of the commands this library emits; unknown commands fail loudly.
 * Uses:      Hex.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-06
 */
package io.github.someoneanoncoder.escposindic.testing;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Describes a byte stream made by this library, one command per line. */
public final class StreamListing {

  private StreamListing() {}

  /**
   * Describes the stream.
   *
   * @param data a stream produced by the library
   * @return one line per command, text and image
   * @throws IllegalArgumentException on a command the library never emits
   */
  public static String describe(byte[] data) {
    StringBuilder out = new StringBuilder();
    ByteArrayOutputStream text = new ByteArrayOutputStream();
    int i = 0;
    while (i < data.length) {
      int b = data[i] & 0xFF;
      if (b == 0x1B || b == 0x1D) {
        flushText(text, out, false);
        i += command(data, i, out);
      } else if (b == 0x0A) {
        flushText(text, out, true);
        i++;
      } else {
        text.write(b);
        i++;
      }
    }
    flushText(text, out, false);
    return out.toString();
  }

  private static int command(byte[] d, int i, StringBuilder out) {
    int lead = d[i] & 0xFF;
    int code = at(d, i + 1);
    if (lead == 0x1B) {
      return switch (code) {
        case '@' -> simple(out, "INITIALIZE", 2);
        case 'a' -> simple(out, "ALIGN " + at(d, i + 2), 3);
        case 'E' -> simple(out, "BOLD " + at(d, i + 2), 3);
        case '-' -> simple(out, "UNDERLINE " + at(d, i + 2), 3);
        case 'd' -> simple(out, "FEED_LINES " + at(d, i + 2), 3);
        case 'J' -> simple(out, "FEED_UNITS " + at(d, i + 2), 3);
        case '3' -> simple(out, "LINE_SPACING " + at(d, i + 2), 3);
        case '2' -> simple(out, "LINE_SPACING_DEFAULT", 2);
        case 'p' ->
            simple(
                out,
                "DRAWER pin=" + at(d, i + 2) + " on=" + at(d, i + 3) + " off=" + at(d, i + 4),
                5);
        default -> throw new IllegalArgumentException("unknown ESC command " + code);
      };
    }
    return switch (code) {
      case '!' -> simple(out, "SIZE 0x" + Integer.toHexString(at(d, i + 2)), 3);
      case 'V' -> simple(out, "CUT m=" + at(d, i + 2) + " n=" + at(d, i + 3), 4);
      case 'v' -> image(d, i, out);
      default -> throw new IllegalArgumentException("unknown GS command " + code);
    };
  }

  private static int image(byte[] d, int i, StringBuilder out) {
    int widthBytes = at(d, i + 4) | (at(d, i + 5) << 8);
    int rows = at(d, i + 6) | (at(d, i + 7) << 8);
    int length = widthBytes * rows;
    int black = 0;
    for (int k = 0; k < length; k++) {
      black += Integer.bitCount(d[i + 8 + k] & 0xFF);
    }
    out.append("IMAGE bytesPerRow=")
        .append(widthBytes)
        .append(" rows=")
        .append(rows)
        .append(" blackDots=")
        .append(black)
        .append('\n');
    return 8 + length;
  }

  private static int simple(StringBuilder out, String text, int length) {
    out.append(text).append('\n');
    return length;
  }

  private static int at(byte[] d, int index) {
    return d[index] & 0xFF;
  }

  private static void flushText(ByteArrayOutputStream text, StringBuilder out, boolean lineEnd) {
    if (text.size() > 0 || lineEnd) {
      out.append(lineEnd ? "LINE \"" : "TEXT \"")
          .append(text.toString(StandardCharsets.US_ASCII))
          .append("\"\n");
      text.reset();
    }
  }
}
