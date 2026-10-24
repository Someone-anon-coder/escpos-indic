# escpos-indic

ESC/POS thermal receipt printing for Java 25, built for Indian shops: Hindi and Marathi text is shaped by Java2D and printed as a raster image, plain ASCII stays fast native text, and UPI QR codes and barcodes are drawn as images. Works with 58 mm (384 dots) and 80 mm (576 dots) printers over a Windows print queue, TCP port 9100, or into a file for tests.

Status: `0.1.1` (`0.1.0` exported no packages to module-path users; use `0.1.1`). **Not yet verified on a real printer** (see [Not verified](#not-verified)). MIT licence.

- Java 25, JPMS module `io.github.someoneanoncoder.escposindic`.
- One runtime dependency: [ZXing core](https://github.com/zxing/zxing) (Apache-2.0), for QR and barcode matrices.
- No bundled font. You pass the `java.awt.Font`s, so you choose the licence and the look.

## Install

JitPack (first release):

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.Someone-anon-coder:escpos-indic:v0.1.1") }
```

Maven Central coordinates `io.github.someone-anon-coder:escpos-indic` follow before v1.0.0.

## Examples

### 1. A plain ASCII receipt into a file

```java
ReceiptWriter w = ReceiptWriter.asciiOnly(PaperWidth.MM58);
w.commands().initialize();
w.align(Alignment.CENTER).line("SAMPLE HARDWARE");
w.align(Alignment.LEFT).rule('-');
w.row(Column.left(14, "Cement 50kg"), Column.right(4, "2"), Column.right(14, "1,300.00"));
w.rule('-');
w.commands().feed(2).cut(CutMode.PARTIAL);

new FileSinkTransport(Path.of("receipt.bin"), false).send(w.build());
```

Columns are counted in Font A character cells (12 dots), so the same row definition works for native and raster text. 58 mm paper has 32 columns, 80 mm has 48.

### 2. Hindi and Marathi, mixed with Latin text and numbers

Most Indic fonts have no Latin letters, so give the rasteriser a font per script. Each character is drawn with the first font that has it.

```java
Font latin = Font.createFont(Font.TRUETYPE_FONT, latinStream).deriveFont(22f);
Font devanagari = Font.createFont(Font.TRUETYPE_FONT, devanagariStream).deriveFont(24f);
TextRasteriser raster = TextRasteriser.of(latin, PaperWidth.MM80).withFallback(devanagari);

ReceiptWriter w = ReceiptWriter.of(raster);
w.align(Alignment.CENTER).line("धन्यवाद, फिर पधारिए");              // raster (Hindi)
w.line("मराठी: पुन्हा या");                                        // raster (Marathi)
w.align(Alignment.LEFT).row(
    Column.left(30, "सीमेंट 50 किलो"), Column.right(18, "₹1,300.00")); // one raster row
w.line("Thank you");                                                // still native text
```

A line made only of printable ASCII is sent as printer text. Anything else, including the rupee sign, makes that line (or that whole row, so columns stay aligned) a raster. Conjuncts, vowel signs and reordering come from Java2D's shaping engine; use any font you are licensed to ship, for example Noto Sans Devanagari.

### 3. A UPI QR code and barcodes

```java
w.align(Alignment.CENTER).line("Scan to pay");
w.image(BarcodeRaster.qr("upi://pay?pa=shop@bank&am=118.00&cu=INR", QrLevel.M, 288));
w.image(BarcodeRaster.code128("A/26-27/000001", 2, 50));
w.image(BarcodeRaster.ean13("590123412345", 2, 50));   // check digit added: 5901234123457
```

Modules are scaled by whole dots only. The QR is the largest that fits the width you give, with the 4-module quiet zone the standard asks for. The library builds the QR from whatever text you pass; building the UPI link is your application's job.

### 4. Sending

```java
byte[] bytes = w.build();
TcpTransport.rawPrinter("192.168.1.50").send(bytes);                       // port 9100
PrintQueueTransport.named("POS-58", Duration.ofSeconds(15)).orElseThrow()  // Windows queue
    .send(bytes);
```

`PrintQueueTransport.queueNames()` lists the installed queues. Every transport has timeouts and validates its input.

## Behaviour you should know

- **User text cannot inject commands.** Native text accepts printable ASCII (0x20-0x7E) only; control characters such as ESC and GS are refused, never sent. Customer names and item descriptions are safe to pass through.
- **Limits.** Text up to 2,000 characters per raster call, images up to 2,048 x 16,384 dots, QR content up to 1,000 characters, jobs up to 8 MiB. Error messages never repeat your content.
- **QR codes** use the QR standard's 4-module quiet zone. Pure ASCII content is written without an ECI header (smaller code, safest for scanners); non-ASCII content is written as UTF-8 with an ECI header.
- **Tall images** are sent in bands of 255 rows, each its own `GS v 0`.
- **Raster text ignores bold and size commands**; choose the font size instead. About 24 dots is readable on 203 dpi paper.
- Layout assumes the printer's normal Font A and normal character size.

## Commands and sources

Command bytes follow the *ESC/POS Command Reference* by Epson (`https://download4.epson.biz/sec_pubs/pos/reference_en/escpos/`). Each is named in the Javadoc.

| Method | Command | Bytes |
|---|---|---|
| `initialize()` | ESC @ | `1B 40` |
| `align(a)` | ESC a n | `1B 61 n` (0 left, 1 centre, 2 right) |
| `bold(on)` | ESC E n | `1B 45 n` |
| `underline(d)` | ESC - n | `1B 2D n` |
| `size(w, h)` | GS ! n | `1D 21 n`, n = (w-1)<<4 or (h-1) |
| `feed(n)` | ESC d n | `1B 64 n` |
| `feedUnits(n)` | ESC J n | `1B 4A n` |
| `lineSpacing(n)` / `lineSpacingDefault()` | ESC 3 n / ESC 2 | `1B 33 n` / `1B 32` |
| `cut(mode)` | GS V m n (function B) | `1D 56 41 00` full, `1D 56 42 00` partial |
| `drawerKick(pin, on, off)` | ESC p m t1 t2 | `1B 70 m t1 t2`, times in 2 ms units |
| `image(img)` | GS v 0 | `1D 76 30 00 xL xH yL yH data` |
| `text(s)`, `newLine()` | text, LF | ASCII, `0A` |

The expected bytes of every command are written by hand in `src/test/resources/.../golden/commands/`, not generated from the code.

## Not verified

This is what could not be checked against a primary source or real hardware. Treat it as open until a release note says otherwise.

1. **No real printer has been used yet.** Nothing here has been printed on an 80 mm or a 58 mm thermal printer. The v0.1.0 contract is "bytes verified, not yet verified on hardware": the byte streams are covered by golden tests and every picture was inspected on screen. Photographs of real prints on both widths will be added here when a printer is available.
2. **The Epson command reference could not be read** from the build machine (the site answers 403 to scripted requests). The command bytes were cross-checked against the open-source [python-escpos](https://github.com/python-escpos/python-escpos) constants (MIT) and against the command names and parameter layouts published by Epson. Re-check against the current reference before relying on rarely used printers.
3. **`GS v 0` is marked obsolete by Epson** in favour of `GS ( L`. Common receipt printers still implement it; printers that do not will print nothing or garbage. Banding at 255 rows is a conservative choice, not a measured one.
4. **Dot widths** 384 (58 mm) and 576 (80 mm) with 12-dot Font A columns are the usual 203 dpi figures. Some printers print narrower (for example 512 dots on 80 mm paper) or use other dpi; only these two widths exist today.
5. **Windows print queue:** sending `DocFlavor.BYTE_ARRAY.AUTOSENSE` through `javax.print` is expected to pass the bytes to the spooler as raw data, but this was only tested against a fake queue. Whether a particular driver leaves ESC/POS bytes untouched depends on the driver.
6. **TCP** was tested against a local server only, not a printer.
7. **Text rendering** differs slightly between operating systems and JDK builds. Golden pictures are exact on Linux with JDK 25 and compared with a 2 % tolerance elsewhere; Windows output has not been inspected by eye.
8. **Raster threshold** (default 160) was chosen from screen pictures, not from thermal paper. Adjust with `withThreshold`.
9. The Hindi and Marathi sample sentences are illustrative and have not been reviewed by a native speaker.
10. **QR scanning with phones and UPI apps** was not tried. The approved QR pictures decode in OpenCV and, in ZXing's pure-barcode mode, in ZXing; ZXing's default detector failed on about 1 in 100 random codes this library produced (the same images that OpenCV reads), so if you verify prints with a ZXing-based scanner app, expect an occasional miss on screen-clean images. Real thermal prints scan differently from screens; test your printer with the UPI apps your customers use.
11. ESC/POS has many printer-specific behaviours (cut position offsets, drawer pulse timing, partial cut support). The library sends standard commands and does not detect printer capabilities.

## Development

```
./gradlew check            # format, checkstyle, all tests, coverage gate (90 %)
./gradlew pitest           # mutation testing (gate 70 %)
./gradlew testGolden -PupdateGolden   # rewrite approved goldens (not the hand-written commands)
```

The test resources contain Noto Sans and Noto Sans Devanagari (SIL Open Font License 1.1, in `src/test/resources/.../fonts`); they are test-only and are not in the published jar.

See `CONTRIBUTING.md`, `SECURITY.md` and `CHANGELOG.md`.
