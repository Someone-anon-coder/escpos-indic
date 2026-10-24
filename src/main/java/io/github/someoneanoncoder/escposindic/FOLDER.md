# escposindic (package root)

**Path:** src/main/java/io/github/someoneanoncoder/escposindic
**Purpose:** Public API types.

## Contents
| File | Purpose | Plan |
|---|---|---|
| EscPosBuilder.java | Typed ESC/POS commands and printable-ASCII text into bytes | L3-01 |
| BitImage.java | Immutable 1-bit raster image, composition, thresholding | L3-02 |
| PaperWidth.java | 58 mm / 80 mm dot widths and columns | L3-01 |
| Alignment.java | ESC a justification codes | L3-01 |
| CutMode.java | GS V function B cut modes | L3-01 |
| DrawerPin.java | ESC p drawer pins | L3-01 |
| package-info.java | Package documentation | L3 |

## Sub-folders
- raster/ — text to 1-bit image with Java2D
- internal/ — non-exported implementations (when needed)

## Rules for adding files here
- Public API nouns only; implementations go in internal/.
