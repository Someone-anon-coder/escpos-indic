# fonts

**Path:** src/test/resources/io/github/someoneanoncoder/escposindic/fonts
**Layer:** test resources
**Purpose:** Test-only Devanagari font so rasteriser tests and golden PNGs run on any machine. Never part of the published jar. SIL Open Font License 1.1.

## Contents
| File | Purpose | Plan |
|---|---|---|
| NotoSansDevanagari-Regular.ttf | Hindi/Marathi regular, unmodified upstream file (Debian fonts-noto-core 20201225-2) | L3-02 |
| OFL.txt | Licence text and provenance | L3-02 |

## Rules for adding files here
- Test-only. The library takes a `java.awt.Font` as a parameter and bundles no font.
