/*
 * File:      module-info.java
 * Module:    escpos-indic
 * Layer:     api
 * Purpose:   JPMS descriptor exporting only the public API packages.
 * Owns:      The list of exported packages and required modules.
 * Uses:      java.desktop (Java2D text rendering, javax.print), ZXing core (barcode matrices).
 * Used by:   Every consumer on the module path.
 * Not here:  Implementation classes (the internal package is not exported).
 * Reuse:     Keep exports minimal.
 * Plan:      L3
 */

/** ESC/POS thermal printing with rasterised Hindi/Marathi text, QR codes and barcodes. */
@SuppressWarnings(
    "requires-automatic") // ZXing core ships no module descriptor; it is an automatic module.
module io.github.someoneanoncoder.escposindic {
  requires transitive java.desktop; // BufferedImage and Font appear in the public API
  requires com.google.zxing;

  exports io.github.someoneanoncoder.escposindic;
  exports io.github.someoneanoncoder.escposindic.barcode;
  exports io.github.someoneanoncoder.escposindic.layout;
  exports io.github.someoneanoncoder.escposindic.raster;
  exports io.github.someoneanoncoder.escposindic.transport;
}
