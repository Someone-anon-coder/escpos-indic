/*
 * File:      FileSinkTransportSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves the file sink will not follow links or write into directories.
 * Owns:      Symbolic link and directory refusal.
 * Uses:      FileSinkTransport.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SecurityTest
class FileSinkTransportSecurityTest {

  @TempDir Path dir;

  @Test
  @DisplayName("a directory is refused")
  void shouldRefuse_whenTargetIsDirectory() {
    assertThatThrownBy(() -> new FileSinkTransport(dir, false).send(new byte[] {1}))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("directory");
  }

  @Test
  @DisplayName("a symbolic link is refused and its target stays untouched")
  void shouldRefuse_whenTargetIsSymbolicLink() throws IOException {
    Path target = dir.resolve("precious.txt");
    Files.writeString(target, "keep");
    Path link = dir.resolve("job.bin");
    try {
      Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "symbolic links are not available here");
    }
    assertThatThrownBy(() -> new FileSinkTransport(link, true).send(new byte[] {1}))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("symbolic link");
    assertThat(Files.readString(target)).isEqualTo("keep");
  }
}
