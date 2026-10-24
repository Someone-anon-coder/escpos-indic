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
module io.github.someoneanoncoder.escposindic {
  requires java.desktop;

// Exports are added with the first public types (L3-01).
}
