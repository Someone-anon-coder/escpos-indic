# Changelog

All notable changes are listed here. Format: [Keep a Changelog](https://keepachangelog.com/); versioning: [SemVer](https://semver.org/) (breaking changes only in minor bumps before 1.0.0).

## [Unreleased]

### Added
- Golden tests: hand-written bytes for every command, approved sample receipts at 58 and 80 mm with readable listings, approved PNGs for Hindi rasters and barcodes [L3-06].
- `Transport` with `PrintQueueTransport` (javax.print raw bytes), `TcpTransport` (port 9100, connect and write deadlines) and `FileSinkTransport` (no links, no directories) [L3-05].
- `BarcodeRaster` (QR with whole-dot modules and quiet zone, Code 128, EAN-13 as `BitImage`), `QrLevel`, `BitImage.generate` and `padded`, `ReceiptWriter.image`; ZXing core 3.5.4 is the only runtime dependency; QR codes of ASCII content are written without an ECI header, because ZXing's own reader could not decode some larger ones that had it (found by a property test, readable by OpenCV) [L3-04].
- `ReceiptWriter` and `Column`: native text for ASCII, raster for everything else, column rows, rules; `TextRasteriser.withFallback` for per-character font choice (Latin + Devanagari) [L3-03].
- `BitImage`, `EscPosBuilder.image` (`GS v 0` in bands of 255 rows) and `TextRasteriser` (Java2D shaped Devanagari to 1-bit at 384/576 dots, wrapping, alignment, thresholding) [L3-02].
- `EscPosBuilder`: initialise, align, bold, underline, size, feed, line spacing, cut, drawer kick and printable-ASCII text; `PaperWidth`, `Alignment`, `CutMode`, `DrawerPin` [L3-01].
- Project scaffold: Gradle, JPMS descriptor, Spotless, Checkstyle, JaCoCo, PIT, CI on ubuntu and windows, release workflow [L3].
