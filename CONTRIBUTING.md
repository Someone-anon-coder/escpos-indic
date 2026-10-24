# Contributing

Issues and pull requests are welcome.

1. Open an issue first for new behaviour; this library is deliberately small (ESC/POS bytes, raster text, barcodes, transports).
2. Branch from `main`: `<type>/<area>/<short-desc>`. Never delete branches.
3. Follow `CLAUDE.md`: file header, Javadoc on public API, `FOLDER.md` per folder, files of at most 400 lines.
4. Every change needs tests (unit, plus property or golden where it fits). Run `./gradlew check` and `./gradlew pitest`.
5. Golden files change only with `-PupdateGolden`, and the diff must be reviewed in the PR.
6. Commits use Conventional Commits, for example `feat(commands): add underline`.
7. Runtime dependencies are limited to the JDK and ZXing core. Test and build tools must be permissively licensed.

Every command you add must cite the vendor command reference it comes from, in Javadoc and the README.
