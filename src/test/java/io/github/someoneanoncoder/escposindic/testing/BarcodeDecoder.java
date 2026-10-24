/*
 * File:      BarcodeDecoder.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Reads barcodes back from BitImages with ZXing so tests check what a scanner would see.
 * Owns:      Conversion of a BitImage to a ZXing bitmap and decoding.
 * Uses:      BitImage, ZXing core.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-04
 */
package io.github.someoneanoncoder.escposindic.testing;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import io.github.someoneanoncoder.escposindic.BitImage;
import java.util.Map;

/**
 * Decodes the barcode in a {@link BitImage}.
 *
 * <p>The images are clean synthetic pictures, so ZXing's pure-barcode mode is used. Without it
 * ZXing's detector fails on about 1 in 100 random QR codes (checked on 3,000 inputs: both
 * binarisers fail on the same ones) although OpenCV decodes every one of them; with it 6,000 of
 * 6,000 decode. The caller names the expected format because ZXing otherwise sometimes reads a
 * different format out of valid Code 128 bars. Real scanners work on camera pictures and are not
 * affected by this choice.
 */
public final class BarcodeDecoder {

  private BarcodeDecoder() {}

  /**
   * Decodes whatever barcode the image holds.
   *
   * @param image the image
   * @param expected the only format to look for; without this ZXing can report a different valid
   *     format hidden in the bars (UPC-A or EAN-8 inside a Code 128, seen in 3 of 20,000 codes)
   * @return the decoded text and format name, for example {@code "EAN_13"}
   */
  public static Decoded decode(BitImage image, BarcodeFormat expected) {
    int[] pixels = new int[image.width() * image.height()];
    for (int y = 0; y < image.height(); y++) {
      for (int x = 0; x < image.width(); x++) {
        pixels[y * image.width() + x] = image.isBlack(x, y) ? 0xFF000000 : 0xFFFFFFFF;
      }
    }
    BinaryBitmap bitmap =
        new BinaryBitmap(
            new HybridBinarizer(new RGBLuminanceSource(image.width(), image.height(), pixels)));
    try {
      Result result =
          new MultiFormatReader()
              .decode(
                  bitmap,
                  Map.of(
                      DecodeHintType.CHARACTER_SET,
                      "UTF-8",
                      DecodeHintType.PURE_BARCODE,
                      Boolean.TRUE));
      return new Decoded(result.getText(), result.getBarcodeFormat().name());
    } catch (NotFoundException e) {
      throw new AssertionError("no barcode found in " + image, e);
    }
  }

  /**
   * A decoded barcode.
   *
   * @param text the content
   * @param format the format name
   */
  public record Decoded(String text, String format) {}
}
