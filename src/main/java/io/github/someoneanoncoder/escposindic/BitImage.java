/*
 * File:      BitImage.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   Immutable 1-bit raster image in the layout ESC/POS raster commands expect.
 * Owns:      Bit packing, size limits, thresholding from a Java2D image, side-by-side and stacked composition.
 * Uses:      java.awt.image.BufferedImage.
 * Used by:   EscPosBuilder.image, TextRasteriser, barcode rasters, the line layout.
 * Not here:  Drawing text (TextRasteriser), sending bytes (transports).
 * Reuse:     The one raster type for every image the library prints.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A black-and-white image, one bit per dot, rows packed most significant bit first, a set bit is a
 * black dot. Each row is padded with white dots to a whole number of bytes, the layout of the
 * {@code GS v 0} raster bit image command.
 *
 * <p>Sizes are bounded ({@value #MAX_WIDTH} by {@value #MAX_HEIGHT} dots) so that untrusted sizes
 * cannot exhaust memory. Instances are immutable.
 */
public final class BitImage {

  /** Largest accepted width in dots. */
  public static final int MAX_WIDTH = 2048;

  /** Largest accepted height in dots. */
  public static final int MAX_HEIGHT = 16384;

  private static final int LUMA_RED = 299;
  private static final int LUMA_GREEN = 587;
  private static final int LUMA_BLUE = 114;
  private static final int LUMA_SCALE = 1000;

  private final int width;
  private final int height;
  private final byte[] data;

  private BitImage(int width, int height, byte[] data) {
    this.width = width;
    this.height = height;
    this.data = data;
  }

  /**
   * Creates an all-white image.
   *
   * @param width width in dots, 1 to {@value #MAX_WIDTH}
   * @param height height in dots, 1 to {@value #MAX_HEIGHT}
   * @return the blank image
   * @throws IllegalArgumentException if a size is out of range
   */
  public static BitImage blank(int width, int height) {
    checkSize(width, height);
    return new BitImage(width, height, new byte[rowBytes(width) * height]);
  }

  /**
   * Creates an image from packed rows. The array is copied and the padding bits at the end of each
   * row are cleared, so equal pictures are always equal images.
   *
   * @param width width in dots, 1 to {@value #MAX_WIDTH}
   * @param height height in dots, 1 to {@value #MAX_HEIGHT}
   * @param packed rows of {@code (width + 7) / 8} bytes, most significant bit first
   * @return the image
   * @throws IllegalArgumentException if a size is out of range or {@code packed} has the wrong
   *     length
   */
  public static BitImage of(int width, int height, byte[] packed) {
    checkSize(width, height);
    Objects.requireNonNull(packed, "packed");
    int rowBytes = rowBytes(width);
    if (packed.length != rowBytes * height) {
      throw new IllegalArgumentException(
          "packed data must be " + (rowBytes * height) + " bytes but was " + packed.length);
    }
    byte[] copy = packed.clone();
    int padBits = rowBytes * 8 - width;
    if (padBits > 0) {
      int keep = (0xFF << padBits) & 0xFF;
      for (int y = 0; y < height; y++) {
        copy[y * rowBytes + rowBytes - 1] &= (byte) keep;
      }
    }
    return new BitImage(width, height, copy);
  }

  /** Tells which dots of a generated image are black. */
  @FunctionalInterface
  public interface DotSource {
    /**
     * Tells whether one dot is black.
     *
     * @param x column, 0 to width - 1
     * @param y row, 0 to height - 1
     * @return true for black
     */
    boolean isBlack(int x, int y);
  }

  /**
   * Creates an image by asking a function for every dot.
   *
   * @param width width in dots, 1 to {@value #MAX_WIDTH}
   * @param height height in dots, 1 to {@value #MAX_HEIGHT}
   * @param source tells which dots are black, not null
   * @return the image
   * @throws IllegalArgumentException if a size is out of range
   */
  public static BitImage generate(int width, int height, DotSource source) {
    Objects.requireNonNull(source, "source");
    checkSize(width, height);
    int rowBytes = rowBytes(width);
    byte[] bits = new byte[rowBytes * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (source.isBlack(x, y)) {
          bits[y * rowBytes + (x >> 3)] |= (byte) (0x80 >> (x & 7));
        }
      }
    }
    return new BitImage(width, height, bits);
  }

  /**
   * Converts a Java2D image to black and white. A dot is black when its brightness, after blending
   * with white by its alpha, is below the threshold.
   *
   * @param image the source, not null
   * @param threshold 1 to 255; brightness below this value is black (128 is the middle)
   * @return the 1-bit image
   * @throws IllegalArgumentException if the size or the threshold is out of range
   */
  public static BitImage fromImage(BufferedImage image, int threshold) {
    Objects.requireNonNull(image, "image");
    if (threshold < 1 || threshold > 255) {
      throw new IllegalArgumentException("threshold must be 1 to 255 but was " + threshold);
    }
    int w = image.getWidth();
    int h = image.getHeight();
    checkSize(w, h);
    int rowBytes = rowBytes(w);
    byte[] bits = new byte[rowBytes * h];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (brightness(image.getRGB(x, y)) < threshold) {
          bits[y * rowBytes + (x >> 3)] |= (byte) (0x80 >> (x & 7));
        }
      }
    }
    return new BitImage(w, h, bits);
  }

  /**
   * Places images next to each other, top aligned. Shorter images are padded with white at the
   * bottom.
   *
   * @param parts at least one image, left to right
   * @return the combined image
   * @throws IllegalArgumentException if there are no parts or the result exceeds the size limits
   */
  public static BitImage sideBySide(List<BitImage> parts) {
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("at least one image is required");
    }
    int totalWidth = 0;
    int maxHeight = 0;
    for (BitImage part : parts) {
      totalWidth += part.width;
      maxHeight = Math.max(maxHeight, part.height);
    }
    BitImage result = blank(totalWidth, maxHeight);
    int xOffset = 0;
    for (BitImage part : parts) {
      result.copyFrom(part, xOffset, 0);
      xOffset += part.width;
    }
    return result;
  }

  /**
   * Places images above each other, left aligned. Narrower images are padded with white on the
   * right.
   *
   * @param parts at least one image, top to bottom
   * @return the combined image
   * @throws IllegalArgumentException if there are no parts or the result exceeds the size limits
   */
  public static BitImage stack(List<BitImage> parts) {
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("at least one image is required");
    }
    int maxWidth = 0;
    int totalHeight = 0;
    for (BitImage part : parts) {
      maxWidth = Math.max(maxWidth, part.width);
      totalHeight += part.height;
    }
    BitImage result = blank(maxWidth, totalHeight);
    int yOffset = 0;
    for (BitImage part : parts) {
      result.copyFrom(part, 0, yOffset);
      yOffset += part.height;
    }
    return result;
  }

  /**
   * Returns the image placed on a white strip of a given width, so that it sits left, centred or
   * right on the paper regardless of the printer's justification setting.
   *
   * @param targetWidth width of the strip in dots; at least the image width, at most {@value
   *     #MAX_WIDTH}
   * @param alignment where the image sits, not null
   * @return a new image {@code targetWidth} wide, or this image if it already has that width
   * @throws IllegalArgumentException if the image is wider than {@code targetWidth}
   */
  public BitImage padded(int targetWidth, Alignment alignment) {
    Objects.requireNonNull(alignment, "alignment");
    if (targetWidth < width) {
      throw new IllegalArgumentException(
          "image is " + width + " dots wide but the target is " + targetWidth);
    }
    if (targetWidth == width) {
      return this;
    }
    int slack = targetWidth - width;
    int offset =
        switch (alignment) {
          case LEFT -> 0;
          case CENTER -> slack / 2;
          case RIGHT -> slack;
        };
    BitImage strip = blank(targetWidth, height);
    strip.copyFrom(this, offset, 0);
    return strip;
  }

  /**
   * Returns the width in dots.
   *
   * @return 1 to {@value #MAX_WIDTH}
   */
  public int width() {
    return width;
  }

  /**
   * Returns the height in dots.
   *
   * @return 1 to {@value #MAX_HEIGHT}
   */
  public int height() {
    return height;
  }

  /**
   * Returns the number of bytes in one packed row.
   *
   * @return {@code (width + 7) / 8}
   */
  public int rowBytes() {
    return rowBytes(width);
  }

  /**
   * Tells whether a dot is black.
   *
   * @param x column, 0 to width - 1
   * @param y row, 0 to height - 1
   * @return true for black
   * @throws IndexOutOfBoundsException if the position is outside the image
   */
  public boolean isBlack(int x, int y) {
    Objects.checkIndex(x, width);
    Objects.checkIndex(y, height);
    return (data[y * rowBytes(width) + (x >> 3)] & (0x80 >> (x & 7))) != 0;
  }

  /**
   * Returns the packed rows.
   *
   * @return a copy of {@code rowBytes() * height()} bytes
   */
  public byte[] toBytes() {
    return data.clone();
  }

  /**
   * Copies the rows {@code from} (inclusive) to {@code to} (exclusive) as packed bytes. Used to
   * send a tall image in bands.
   *
   * @param from first row
   * @param to row after the last
   * @return the packed bytes of those rows
   * @throws IndexOutOfBoundsException if the range is outside the image
   */
  public byte[] rowsToBytes(int from, int to) {
    Objects.checkFromToIndex(from, to, height);
    int rowBytes = rowBytes(width);
    return Arrays.copyOfRange(data, from * rowBytes, to * rowBytes);
  }

  /**
   * Draws the image as a black-on-white picture.
   *
   * @return a new binary image of the same size, black dots as black pixels
   */
  public BufferedImage toBufferedImage() {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, isBlack(x, y) ? 0xFF000000 : 0xFFFFFFFF);
      }
    }
    return image;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof BitImage that
        && width == that.width
        && height == that.height
        && Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(width, height) + Arrays.hashCode(data);
  }

  @Override
  public String toString() {
    return "BitImage[" + width + "x" + height + "]";
  }

  private void copyFrom(BitImage part, int xOffset, int yOffset) {
    for (int y = 0; y < part.height; y++) {
      for (int x = 0; x < part.width; x++) {
        if (part.isBlack(x, y)) {
          int dx = x + xOffset;
          data[(y + yOffset) * rowBytes(width) + (dx >> 3)] |= (byte) (0x80 >> (dx & 7));
        }
      }
    }
  }

  private static int brightness(int argb) {
    int alpha = argb >>> 24;
    int red = blendWithWhite((argb >> 16) & 0xFF, alpha);
    int green = blendWithWhite((argb >> 8) & 0xFF, alpha);
    int blue = blendWithWhite(argb & 0xFF, alpha);
    return (red * LUMA_RED + green * LUMA_GREEN + blue * LUMA_BLUE) / LUMA_SCALE;
  }

  private static int blendWithWhite(int channel, int alpha) {
    return (channel * alpha + 255 * (255 - alpha)) / 255;
  }

  private static int rowBytes(int width) {
    return (width + 7) >> 3;
  }

  private static void checkSize(int width, int height) {
    if (width < 1 || width > MAX_WIDTH) {
      throw new IllegalArgumentException("width must be 1 to " + MAX_WIDTH + " but was " + width);
    }
    if (height < 1 || height > MAX_HEIGHT) {
      throw new IllegalArgumentException(
          "height must be 1 to " + MAX_HEIGHT + " but was " + height);
    }
  }
}
