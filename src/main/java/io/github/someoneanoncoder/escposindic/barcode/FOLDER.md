# barcode

**Path:** src/main/java/io/github/someoneanoncoder/escposindic/barcode
**Purpose:** QR, Code 128 and EAN-13 drawn as 1-bit images with ZXing (printed with `GS v 0`, no printer-native barcode commands).

## Contents
| File | Purpose | Plan |
|---|---|---|
| BarcodeRaster.java | QR, Code 128 and EAN-13 to BitImage with whole-dot module scaling | L3-04 |
| QrLevel.java | QR error correction levels | L3-04 |
| package-info.java | Package documentation | L3-04 |

## Depends on
- ZXing core (Apache-2.0), BitImage

## Rules for adding files here
- ZXing types never appear in the public API.
