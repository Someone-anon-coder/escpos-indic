# testing

**Path:** src/test/java/io/github/someoneanoncoder/escposindic/testing
**Layer:** test support
**Purpose:** Test category meta-annotations and shared test helpers.

## Contents
| File | Purpose | Plan |
|---|---|---|
| TestFonts.java | Loads the test-only Devanagari font | L3-02 |
| BarcodeDecoder.java | Reads barcodes back from images with ZXing | L3-04 |
| Hex.java | Hex text to bytes and back | L3-01 |
| UnitTest.java, PropertyTest.java, GoldenTest.java, ArchTest.java, SecurityTest.java | JUnit 5 tag meta-annotations | L3 |

## Rules for adding files here
- Test support only; no production code.
