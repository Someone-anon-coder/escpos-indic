/*
 * File:      EscPosBuilder.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   Builds an ESC/POS byte stream from typed commands.
 * Owns:      Command encoding, parameter range checks, the printable-ASCII rule for native text.
 * Uses:      Alignment, CutMode, DrawerPin.
 * Used by:   Callers that print receipts; the line layout (L3-03) and BitImage output (L3-02).
 * Not here:  Raster text, barcodes, sending bytes (transport package), receipt templates.
 * Reuse:     Own implementation; no dependency on another ESC/POS library.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Collects ESC/POS commands and text into a byte array.
 *
 * <p>Every command is documented with its name in the Epson ESC/POS Command Reference (<a
 * href="https://download4.epson.biz/sec_pubs/pos/reference_en/escpos/">download4.epson.biz</a>).
 * The bytes were cross-checked against the python-escpos constants because the Epson site could not
 * be read from the build machine; see the README under "Not verified".
 *
 * <p>Native text is limited to printable ASCII (0x20 to 0x7E). Anything else, including control
 * characters such as ESC and GS, is refused so that a customer name or item description can never
 * smuggle a printer command into the stream. Text outside ASCII is printed as a raster image by the
 * rasteriser instead.
 *
 * <p>Instances are mutable and not thread-safe.
 */
public final class EscPosBuilder {

  private static final int ESC = 0x1B;
  private static final int GS = 0x1D;
  private static final int LF = 0x0A;
  private static final int MAX_BYTE = 255;
  private static final int MAX_SIZE = 8;
  private static final int MAX_DRAWER_MILLIS = 510;

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  private EscPosBuilder() {}

  /**
   * Creates an empty builder.
   *
   * @return a new builder with no bytes
   */
  public static EscPosBuilder create() {
    return new EscPosBuilder();
  }

  /**
   * Initialises the printer, command {@code ESC @}: clears the print buffer and resets modes such
   * as bold, size and alignment to their defaults.
   *
   * @return this builder
   */
  public EscPosBuilder initialize() {
    return bytes(ESC, '@');
  }

  /**
   * Selects justification for the following lines, command {@code ESC a n}.
   *
   * @param alignment the alignment, not null
   * @return this builder
   */
  public EscPosBuilder align(Alignment alignment) {
    Objects.requireNonNull(alignment, "alignment");
    return bytes(ESC, 'a', alignment.code());
  }

  /**
   * Turns emphasised (bold) mode on or off, command {@code ESC E n}.
   *
   * @param on true for bold
   * @return this builder
   */
  public EscPosBuilder bold(boolean on) {
    return bytes(ESC, 'E', on ? 1 : 0);
  }

  /**
   * Sets underline thickness, command {@code ESC - n}.
   *
   * @param dots 0 for off, 1 for a one-dot underline, 2 for a two-dot underline
   * @return this builder
   * @throws IllegalArgumentException if {@code dots} is not 0, 1 or 2
   */
  public EscPosBuilder underline(int dots) {
    if (dots < 0 || dots > 2) {
      throw new IllegalArgumentException("underline must be 0, 1 or 2 but was " + dots);
    }
    return bytes(ESC, '-', dots);
  }

  /**
   * Sets the character size as whole-number multiples, command {@code GS ! n}. Bits 4 to 6 of
   * {@code n} hold the width multiple minus one and bits 0 to 2 the height multiple minus one.
   *
   * @param widthMultiple 1 to 8 times the normal width
   * @param heightMultiple 1 to 8 times the normal height
   * @return this builder
   * @throws IllegalArgumentException if a multiple is outside 1 to 8
   */
  public EscPosBuilder size(int widthMultiple, int heightMultiple) {
    requireRange("widthMultiple", widthMultiple, 1, MAX_SIZE);
    requireRange("heightMultiple", heightMultiple, 1, MAX_SIZE);
    return bytes(GS, '!', ((widthMultiple - 1) << 4) | (heightMultiple - 1));
  }

  /**
   * Prints the buffered line and feeds paper by a number of character lines, command {@code ESC d
   * n}.
   *
   * @param lines 0 to 255 lines
   * @return this builder
   * @throws IllegalArgumentException if {@code lines} is outside 0 to 255
   */
  public EscPosBuilder feed(int lines) {
    requireRange("lines", lines, 0, MAX_BYTE);
    return bytes(ESC, 'd', lines);
  }

  /**
   * Prints the buffered line and feeds paper by a number of motion units, command {@code ESC J n}.
   * On 203 dpi printers one unit is typically one dot row; this is not guaranteed by the command
   * set.
   *
   * @param units 0 to 255 units
   * @return this builder
   * @throws IllegalArgumentException if {@code units} is outside 0 to 255
   */
  public EscPosBuilder feedUnits(int units) {
    requireRange("units", units, 0, MAX_BYTE);
    return bytes(ESC, 'J', units);
  }

  /**
   * Sets the line spacing in motion units, command {@code ESC 3 n}.
   *
   * @param units 0 to 255 units
   * @return this builder
   * @throws IllegalArgumentException if {@code units} is outside 0 to 255
   */
  public EscPosBuilder lineSpacing(int units) {
    requireRange("units", units, 0, MAX_BYTE);
    return bytes(ESC, '3', units);
  }

  /**
   * Restores the printer's default line spacing, command {@code ESC 2}.
   *
   * @return this builder
   */
  public EscPosBuilder lineSpacingDefault() {
    return bytes(ESC, '2');
  }

  /**
   * Feeds paper to the cutting position and cuts, command {@code GS V m 0} (function B).
   *
   * @param mode full or partial, not null
   * @return this builder
   */
  public EscPosBuilder cut(CutMode mode) {
    Objects.requireNonNull(mode, "mode");
    return bytes(GS, 'V', mode.code(), 0);
  }

  /**
   * Sends a pulse to the cash drawer connector, command {@code ESC p m t1 t2}. The printer takes
   * the on and off times in units of 2 ms, so odd millisecond values are rounded up.
   *
   * @param pin the connector pin, not null
   * @param onMillis pulse on time, 0 to 510 ms
   * @param offMillis pulse off time, 0 to 510 ms
   * @return this builder
   * @throws IllegalArgumentException if a time is outside 0 to 510
   */
  public EscPosBuilder drawerKick(DrawerPin pin, int onMillis, int offMillis) {
    Objects.requireNonNull(pin, "pin");
    requireRange("onMillis", onMillis, 0, MAX_DRAWER_MILLIS);
    requireRange("offMillis", offMillis, 0, MAX_DRAWER_MILLIS);
    return bytes(ESC, 'p', pin.code(), (onMillis + 1) / 2, (offMillis + 1) / 2);
  }

  /**
   * Sends a 100 ms on, 100 ms off pulse to the cash drawer.
   *
   * @param pin the connector pin, not null
   * @return this builder
   */
  public EscPosBuilder drawerKick(DrawerPin pin) {
    return drawerKick(pin, 100, 100);
  }

  /**
   * Adds native text to the print buffer. Only printable ASCII is accepted; the line is not ended.
   *
   * @param text printable ASCII (0x20 to 0x7E), not null
   * @return this builder
   * @throws IllegalArgumentException if {@code text} holds any other character, including line
   *     breaks and control characters
   */
  public EscPosBuilder text(String text) {
    Objects.requireNonNull(text, "text");
    for (int i = 0; i < text.length(); i++) {
      if (!isPrintableAscii(text.charAt(i))) {
        throw new IllegalArgumentException(
            "native text allows printable ASCII only; refused character at index " + i);
      }
    }
    out.writeBytes(text.getBytes(StandardCharsets.US_ASCII));
    return this;
  }

  /**
   * Adds native text followed by a line feed ({@code LF}).
   *
   * @param text printable ASCII, not null
   * @return this builder
   * @throws IllegalArgumentException as for {@link #text(String)}
   */
  public EscPosBuilder line(String text) {
    return text(text).newLine();
  }

  /**
   * Prints the buffer and advances one line, the {@code LF} control code.
   *
   * @return this builder
   */
  public EscPosBuilder newLine() {
    return bytes(LF);
  }

  /**
   * Tells whether a character may be sent as native text.
   *
   * @param c the character
   * @return true for 0x20 to 0x7E
   */
  public static boolean isPrintableAscii(char c) {
    return c >= 0x20 && c <= 0x7E;
  }

  /**
   * Returns the bytes collected so far.
   *
   * @return a copy of the stream
   */
  public byte[] build() {
    return out.toByteArray();
  }

  /**
   * Returns how many bytes have been collected.
   *
   * @return the stream length
   */
  public int length() {
    return out.size();
  }

  private EscPosBuilder bytes(int... values) {
    for (int v : values) {
      out.write(v);
    }
    return this;
  }

  private static void requireRange(String name, int value, int min, int max) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(
          name + " must be " + min + " to " + max + " but was " + value);
    }
  }
}
