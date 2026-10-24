# CLAUDE.md — escpos-indic

Public MIT library (derived from the ShopSuite monorepo CLAUDE.md §12 template). Scope: ESC/POS command builder, text rasteriser (Java2D to 1-bit, `GS v 0`), mixed line layout and column helpers, QR/Code128/EAN-13 rasters, transports (print queue, TCP 9100, file sink). ShopSuite receipt templates are out of scope and stay in the private product.

## Rules

1. Runtime dependencies: the JDK (`java.desktop` for Java2D and `javax.print`) and ZXing core only (Apache-2.0). Test/build tools must be permissively licensed.
2. The library bundles no font. Callers pass a `java.awt.Font`. The test-only Noto Sans Devanagari (SIL OFL-1.1) lives in `src/test/resources` and never ships in the jar.
3. Every command byte cites its source (vendor ESC/POS reference command name) in Javadoc and the README. Anything not verified against a primary source or a real printer is listed in the README under "Not verified".
4. User text never reaches the printer as raw control bytes: native text accepts printable ASCII only; everything else is rasterised or rejected.
5. Every public type/method has full Javadoc; every source file has the header (checkstyle enforced); every folder has `FOLDER.md`.
6. Files at most 400 lines, methods at most 40.
7. Public API uses natural names (`align`, `cut`, `feed`); the monorepo subject-stem rule (§5.3) applies to layered code, not to this API.
8. Tests: `<Class><Category>Test`; categories unit, property, golden, arch, security. Line coverage at least 90%, PIT at least 70%. Golden byte files are hex text; expected values for command bytes are written by hand from the command reference, never generated from the implementation. Golden PNGs are generated on Linux/JDK 25 and compared with a small tolerance elsewhere.
9. Never delete branches. Ask the owner before pushing, opening PRs or publishing releases.
10. Releases only from tags `v*` via CI. Breaking changes only in a minor bump before 1.0.0, listed in CHANGELOG.

## Tracking

Plan IDs L3-01..L3-06 live in the ShopSuite repo `docs/PLAN.md`. Status of this library is tracked in that repo's CLAUDE.md §10.
