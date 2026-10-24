/*
 * File:      BarcodeRaster.java
 * Module:    escpos-indic
 * Layer:     barcode
 * Purpose:   Draws QR codes, Code 128 and EAN-13 barcodes as 1-bit images.
 * Owns:      Input limits, integer scaling of modules, conversion of ZXing matrices to BitImage.
 * Uses:      ZXing core (QRCodeWriter, Code128Writer, EAN13Writer), BitImage.
 * Used by:   Callers printing UPI QR codes and item barcodes; ReceiptWriter.image.
 * Not here:  Printer-native barcode commands (GS k); decoding.
 * Reuse:     Static methods; thread-safe.
 * Plan:      L3-04
 */
package io.github.someoneanoncoder.escposindic.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import io.github.someoneanoncoder.escposindic.BitImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Renders barcodes to {@link BitImage}s with ZXing. Everything is printed as a raster image, so it
 * works on any printer that supports {@code GS v 0}, with no dependency on the printer's built-in
 * barcode fonts.
 *
 * <p>Modules (the narrowest bar or square) are scaled by whole numbers of dots only, because
 * fractional scaling blurs edges and makes scanners fail. On 203 dpi printers a module of at least
 * 2 dots is needed for reliable scanning; 3 dots is safer.
 *
 * <p>Inputs are bounded: QR content up to {@value #MAX_QR_CHARACTERS} characters, Code 128 data up
 * to {@value #MAX_CODE128_CHARACTERS} printable ASCII characters. Error messages never repeat the
 * content, which may be a payment address or an item code.
 */
public final class BarcodeRaster {

  /** Longest QR content accepted. */
  public static final int MAX_QR_CHARACTERS = 1000;

  /** Longest Code 128 data accepted. */
  public static final int MAX_CODE128_CHARACTERS = 60;

  /** Tallest linear barcode accepted, in dots. */
  public static final int MAX_BAR_HEIGHT = 512;

  /** Widest module of a linear barcode accepted, in dots. */
  public static final int MAX_MODULE_DOTS = 8;

  /** Quiet zone around a QR code in modules, as the QR standard asks (ISO/IEC 18004: 4). */
  private static final int QR_QUIET_ZONE = 4;

  private BarcodeRaster() {}

  /**
   * Draws a QR code, as large as fits in {@code maxWidthDots} with whole-dot modules, including the
   * quiet zone. Typical use is a UPI payment link such as {@code upi://pay?pa=shop@bank&am=118.00}.
   *
   * @param content the text to encode (UTF-8), 1 to {@value #MAX_QR_CHARACTERS} characters
   * @param level error correction level, not null
   * @param maxWidthDots the most dots the image may use, such as the paper width
   * @return a square image no wider than {@code maxWidthDots}
   * @throws IllegalArgumentException if the content is empty or too long, cannot be encoded, or the
   *     code does not fit even with 1-dot modules
   */
  public static BitImage qr(String content, QrLevel level, int maxWidthDots) {
    Objects.requireNonNull(level, "level");
    requireLength(content, MAX_QR_CHARACTERS, "QR content");
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, level.zxing());
    hints.put(EncodeHintType.MARGIN, QR_QUIET_ZONE);
    if (!isAscii(content)) {
      // The hint makes ZXing add an ECI header, which costs modules and which some scanners handle
      // poorly. Plain ASCII, such as every UPI link, is therefore written without it.
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    }
    BitMatrix matrix =
        encode(() -> new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints));
    int modules = matrix.getWidth();
    int scale = maxWidthDots / modules;
    if (scale < 1) {
      throw new IllegalArgumentException(
          "QR code needs " + modules + " dots but only " + maxWidthDots + " are available");
    }
    return scaled(matrix, scale, scale * matrix.getHeight());
  }

  /**
   * Draws a Code 128 barcode (ISO/IEC 15417), choosing the code sets that make it shortest.
   *
   * @param data printable ASCII (0x20 to 0x7E), 1 to {@value #MAX_CODE128_CHARACTERS} characters
   * @param moduleDots width of the narrowest bar, 1 to {@value #MAX_MODULE_DOTS} dots
   * @param heightDots height of the bars, 1 to {@value #MAX_BAR_HEIGHT} dots
   * @return the image, including the quiet zones
   * @throws IllegalArgumentException if an argument is out of range or the data cannot be encoded
   */
  public static BitImage code128(String data, int moduleDots, int heightDots) {
    requireLength(data, MAX_CODE128_CHARACTERS, "Code 128 data");
    for (int i = 0; i < data.length(); i++) {
      if (data.charAt(i) < 0x20 || data.charAt(i) > 0x7E) {
        throw new IllegalArgumentException("Code 128 data allows printable ASCII only");
      }
    }
    requireBar(moduleDots, heightDots);
    BitMatrix row = encode(() -> new Code128Writer().encode(data, BarcodeFormat.CODE_128, 0, 1));
    return scaled(row, moduleDots, heightDots);
  }

  /**
   * Draws an EAN-13 barcode (ISO/IEC 15420), the 13-digit retail code.
   *
   * @param digits 12 digits (the check digit is added) or 13 digits (the check digit is verified)
   * @param moduleDots width of the narrowest bar, 1 to {@value #MAX_MODULE_DOTS} dots
   * @param heightDots height of the bars, 1 to {@value #MAX_BAR_HEIGHT} dots
   * @return the image, including the quiet zones
   * @throws IllegalArgumentException if an argument is out of range, the digits are not 12 or 13
   *     digits, or the check digit is wrong
   */
  public static BitImage ean13(String digits, int moduleDots, int heightDots) {
    Objects.requireNonNull(digits, "digits");
    if (!digits.matches("[0-9]{12,13}")) {
      throw new IllegalArgumentException("EAN-13 needs 12 or 13 digits");
    }
    requireBar(moduleDots, heightDots);
    BitMatrix row = encode(() -> new EAN13Writer().encode(digits, BarcodeFormat.EAN_13, 0, 1));
    return scaled(row, moduleDots, heightDots);
  }

  private interface Encoding {
    BitMatrix run() throws WriterException;
  }

  private static BitMatrix encode(Encoding encoding) {
    try {
      return encoding.run();
    } catch (WriterException | IllegalArgumentException e) {
      throw new IllegalArgumentException("the content cannot be encoded as this barcode type");
    }
  }

  private static BitImage scaled(BitMatrix matrix, int scaleX, int heightDots) {
    int scaleY = Math.max(1, heightDots / matrix.getHeight());
    int width = matrix.getWidth() * scaleX;
    int lastRow = matrix.getHeight() - 1;
    return BitImage.generate(
        width, heightDots, (x, y) -> matrix.get(x / scaleX, Math.min(y / scaleY, lastRow)));
  }

  private static boolean isAscii(String text) {
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) > 0x7F) {
        return false;
      }
    }
    return true;
  }

  private static void requireLength(String text, int max, String what) {
    Objects.requireNonNull(text, "text");
    if (text.isEmpty() || text.length() > max) {
      throw new IllegalArgumentException(what + " must be 1 to " + max + " characters");
    }
  }

  private static void requireBar(int moduleDots, int heightDots) {
    if (moduleDots < 1 || moduleDots > MAX_MODULE_DOTS) {
      throw new IllegalArgumentException("moduleDots must be 1 to " + MAX_MODULE_DOTS);
    }
    if (heightDots < 1 || heightDots > MAX_BAR_HEIGHT) {
      throw new IllegalArgumentException("heightDots must be 1 to " + MAX_BAR_HEIGHT);
    }
  }
}
