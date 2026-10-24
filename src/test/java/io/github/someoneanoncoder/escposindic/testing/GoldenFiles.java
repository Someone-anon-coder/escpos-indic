/*
 * File:      GoldenFiles.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Compares output with approved golden files and rewrites them with -PupdateGolden.
 * Owns:      Hex, binary and PNG golden comparison, the Linux-exact rule for text rasters.
 * Uses:      BitImage, ImageIO.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-06
 */
package io.github.someoneanoncoder.escposindic.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.github.someoneanoncoder.escposindic.BitImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Golden file access. Files live in {@code src/test/resources/.../golden}. Run Gradle with {@code
 * -PupdateGolden} to rewrite them; review the diff before committing.
 *
 * <p>Text rasters depend on Java2D's text rendering, which can differ by a few pixels between
 * operating systems. Goldens are generated on Linux with JDK 25: there they must match exactly,
 * elsewhere images may differ in at most {@value #TOLERANCE_PERCENT} percent of the dots.
 */
public final class GoldenFiles {

  /** Share of differing dots tolerated for text rasters off Linux. */
  public static final double TOLERANCE_PERCENT = 2.0;

  private static final Path ROOT =
      Path.of("src/test/resources/io/github/someoneanoncoder/escposindic/golden");

  private GoldenFiles() {}

  /**
   * Tells whether this run is on Linux, where text-raster goldens are exact.
   *
   * @return true on Linux
   */
  public static boolean isReferencePlatform() {
    return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("linux");
  }

  /**
   * Asserts a byte stream equals {@code golden/<name>.hex} (16 upper-case hex bytes per line).
   *
   * @param name path below the golden folder without extension
   * @param actual the produced bytes
   */
  public static void assertHex(String name, byte[] actual) {
    Path file = ROOT.resolve(name + ".hex");
    String text = hexLines(actual);
    // Command goldens are written by hand from the command reference and are never regenerated.
    if (updating() && !name.startsWith("commands/")) {
      write(file, text.getBytes(StandardCharsets.UTF_8));
    }
    assertThat(read(file)).as("golden %s (rewrite with -PupdateGolden)", file).isEqualTo(text);
  }

  /**
   * Asserts a byte stream equals {@code golden/<name>.bin} exactly.
   *
   * @param name path below the golden folder without extension
   * @param actual the produced bytes
   */
  public static void assertBinary(String name, byte[] actual) {
    Path file = ROOT.resolve(name + ".bin");
    if (updating()) {
      write(file, actual);
    }
    assertThat(readBytes(file))
        .as("golden %s (rewrite with -PupdateGolden)", file)
        .isEqualTo(actual);
  }

  /**
   * Reads {@code golden/<name>.bin}.
   *
   * @param name path below the golden folder without extension
   * @return the approved bytes
   */
  public static byte[] readBinary(String name) {
    return readBytes(ROOT.resolve(name + ".bin"));
  }

  /**
   * Asserts text equals {@code golden/<name>.txt}.
   *
   * @param name path below the golden folder without extension
   * @param actual the produced text
   */
  public static void assertText(String name, String actual) {
    Path file = ROOT.resolve(name + ".txt");
    if (updating()) {
      write(file, actual.getBytes(StandardCharsets.UTF_8));
    }
    assertThat(read(file)).as("golden %s (rewrite with -PupdateGolden)", file).isEqualTo(actual);
  }

  /**
   * Asserts an image equals {@code golden/<name>.png}. Exact on Linux; elsewhere the size must
   * match and at most {@value #TOLERANCE_PERCENT} percent of the dots may differ, unless {@code
   * exact}.
   *
   * @param name path below the golden folder without extension
   * @param actual the produced image
   * @param exact true for images that do not depend on text rendering (barcodes)
   */
  public static void assertPng(String name, BitImage actual, boolean exact) {
    Path file = ROOT.resolve(name + ".png");
    if (updating()) {
      try {
        Files.createDirectories(file.getParent());
        ImageIO.write(actual.toBufferedImage(), "png", file.toFile());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    BitImage expected = readPng(file);
    if (exact || isReferencePlatform()) {
      assertThat(actual).as("golden %s (rewrite with -PupdateGolden)", file).isEqualTo(expected);
      return;
    }
    assertThat(actual.width()).isEqualTo(expected.width());
    assertThat(actual.height()).isEqualTo(expected.height());
    assertThat(differingPercent(actual, expected)).isLessThanOrEqualTo(TOLERANCE_PERCENT);
  }

  private static double differingPercent(BitImage a, BitImage b) {
    int diff = 0;
    for (int y = 0; y < a.height(); y++) {
      for (int x = 0; x < a.width(); x++) {
        diff += a.isBlack(x, y) == b.isBlack(x, y) ? 0 : 1;
      }
    }
    return 100.0 * diff / ((double) a.width() * a.height());
  }

  private static BitImage readPng(Path file) {
    try {
      BufferedImage image = ImageIO.read(file.toFile());
      if (image == null) {
        return fail("golden %s is not a readable PNG", file);
      }
      return BitImage.fromImage(image, 128);
    } catch (IOException e) {
      return fail("golden %s is missing; run with -PupdateGolden", file);
    }
  }

  private static String hexLines(byte[] data) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < data.length; i += 16) {
      sb.append(Hex.text(java.util.Arrays.copyOfRange(data, i, Math.min(data.length, i + 16))))
          .append('\n');
    }
    return sb.toString();
  }

  private static boolean updating() {
    return Boolean.getBoolean("updateGolden");
  }

  private static String read(Path file) {
    return new String(readBytes(file), StandardCharsets.UTF_8);
  }

  private static byte[] readBytes(Path file) {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      return fail("golden %s is missing; run with -PupdateGolden", file);
    }
  }

  private static void write(Path file, byte[] data) {
    try {
      Files.createDirectories(file.getParent());
      Files.write(file, data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
