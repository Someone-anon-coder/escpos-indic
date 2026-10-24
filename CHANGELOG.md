# Changelog

All notable changes are listed here. Format: [Keep a Changelog](https://keepachangelog.com/); versioning: [SemVer](https://semver.org/) (breaking changes only in minor bumps before 1.0.0).

## [Unreleased]

### Added
- `BitImage`, `EscPosBuilder.image` (`GS v 0` in bands of 255 rows) and `TextRasteriser` (Java2D shaped Devanagari to 1-bit at 384/576 dots, wrapping, alignment, thresholding) [L3-02].
- `EscPosBuilder`: initialise, align, bold, underline, size, feed, line spacing, cut, drawer kick and printable-ASCII text; `PaperWidth`, `Alignment`, `CutMode`, `DrawerPin` [L3-01].
- Project scaffold: Gradle, JPMS descriptor, Spotless, Checkstyle, JaCoCo, PIT, CI on ubuntu and windows, release workflow [L3].
