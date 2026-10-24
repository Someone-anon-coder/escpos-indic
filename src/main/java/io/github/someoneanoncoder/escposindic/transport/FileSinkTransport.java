/*
 * File:      FileSinkTransport.java
 * Module:    escpos-indic
 * Layer:     transport
 * Purpose:   Writes the byte stream to a file, for tests and for inspecting what a printer would receive.
 * Owns:      File creation, truncate or append, refusal of directories and symbolic links.
 * Uses:      java.nio.file.
 * Used by:   Tests, golden byte-stream capture, diagnostics.
 * Not here:  Printing.
 * Reuse:     Point it at a file the application chose; never at a path taken from untrusted input.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Writes each job to a file. The file is created if missing. A symbolic link or a directory at the
 * path is refused, so a link planted next to a log file cannot redirect the bytes elsewhere.
 *
 * <p>The path is the caller's to choose: do not build it from untrusted input.
 */
public final class FileSinkTransport implements Transport {

  private final Path file;
  private final boolean append;

  /**
   * Creates a sink.
   *
   * @param file the file to write, not null
   * @param append true to add each job to the end, false to replace the file's content per job
   */
  public FileSinkTransport(Path file, boolean append) {
    this.file = Objects.requireNonNull(file, "file");
    this.append = append;
  }

  @Override
  public void send(byte[] data) throws IOException {
    TransportChecks.requireData(data);
    if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("the target is a directory");
    }
    if (Files.isSymbolicLink(file)) {
      throw new IOException("the target is a symbolic link");
    }
    OpenOption mode = append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
    Files.write(
        file,
        data,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        mode,
        LinkOption.NOFOLLOW_LINKS);
  }
}
