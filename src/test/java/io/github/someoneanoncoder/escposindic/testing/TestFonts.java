/*
 * File:      TestFonts.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Loads the test-only Devanagari font.
 * Owns:      Font loading from test resources.
 * Uses:      java.awt.Font.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-02
 */
package io.github.someoneanoncoder.escposindic.testing;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/** The bundled test font (Noto Sans Devanagari Regular, SIL OFL-1.1, test resources only). */
public final class TestFonts {

  private static final String DEVANAGARI =
      "/io/github/someoneanoncoder/escposindic/fonts/NotoSansDevanagari-Regular.ttf";

  private static final String LATIN =
      "/io/github/someoneanoncoder/escposindic/fonts/NotoSans-Regular.ttf";

  private TestFonts() {}

  /**
   * Loads the Latin font (digits, letters, rupee sign) at a size.
   *
   * @param size pixel size, one pixel is one printer dot
   * @return the font
   */
  public static Font latin(float size) {
    return load(LATIN, size);
  }

  /**
   * Loads the Devanagari font at a size.
   *
   * @param size pixel size, one pixel is one printer dot
   * @return the font
   */
  public static Font devanagari(float size) {
    return load(DEVANAGARI, size);
  }

  private static Font load(String resource, float size) {
    try (InputStream in = TestFonts.class.getResourceAsStream(resource)) {
      return Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(in)).deriveFont(size);
    } catch (java.awt.FontFormatException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
