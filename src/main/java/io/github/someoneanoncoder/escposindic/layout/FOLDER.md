# layout

**Path:** src/main/java/io/github/someoneanoncoder/escposindic/layout
**Purpose:** Receipt layout: ASCII-or-raster line choice, column rows, rules.

## Contents
| File | Purpose | Plan |
|---|---|---|
| ReceiptWriter.java | Writes lines and rows, native for ASCII and raster otherwise | L3-03 |
| Column.java | Table cell: width in columns, alignment, text | L3-03 |
| package-info.java | Package documentation | L3-03 |

## Rules for adding files here
- Layout only; command bytes stay in EscPosBuilder.
