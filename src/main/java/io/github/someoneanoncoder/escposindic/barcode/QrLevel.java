/*
 * File:      QrLevel.java
 * Module:    escpos-indic
 * Layer:     barcode
 * Purpose:   QR error correction levels.
 * Owns:      The four levels.
 * Uses:      ZXing ErrorCorrectionLevel (internal mapping).
 * Used by:   BarcodeRaster.qr.
 * Not here:  Drawing.
 * Reuse:     Fixed by ISO/IEC 18004.
 * Plan:      L3-04
 */
package io.github.someoneanoncoder.escposindic.barcode;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * QR code error correction level (ISO/IEC 18004): the share of the code that can be damaged or
 * smudged and still be read. Higher levels make bigger codes. Thermal prints smudge a little, so
 * {@link #M} is a good default.
 */
public enum QrLevel {
  /** About 7 % recoverable. */
  L(ErrorCorrectionLevel.L),
  /** About 15 % recoverable. */
  M(ErrorCorrectionLevel.M),
  /** About 25 % recoverable. */
  Q(ErrorCorrectionLevel.Q),
  /** About 30 % recoverable. */
  H(ErrorCorrectionLevel.H);

  private final ErrorCorrectionLevel level;

  QrLevel(ErrorCorrectionLevel level) {
    this.level = level;
  }

  ErrorCorrectionLevel zxing() {
    return level;
  }
}
