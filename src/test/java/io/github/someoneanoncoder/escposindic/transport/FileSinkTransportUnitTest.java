/*
 * File:      FileSinkTransportUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks the file sink writes, replaces and appends.
 * Owns:      Create, truncate, append, empty and oversize refusal.
 * Uses:      FileSinkTransport.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@UnitTest
class FileSinkTransportUnitTest {

  @TempDir Path dir;

  @Test
  @DisplayName("creates the file with the bytes")
  void shouldCreateFile_whenSent() throws IOException {
    Path file = dir.resolve("job.bin");
    new FileSinkTransport(file, false).send(new byte[] {0x1B, 0x40});
    assertThat(Files.readAllBytes(file)).containsExactly(0x1B, 0x40);
  }

  @Test
  @DisplayName("replace mode keeps only the last job, append mode keeps all")
  void shouldReplaceOrAppend_whenSentTwice() throws IOException {
    Path replaced = dir.resolve("a.bin");
    FileSinkTransport replace = new FileSinkTransport(replaced, false);
    replace.send(new byte[] {1, 2, 3});
    replace.send(new byte[] {9});
    assertThat(Files.readAllBytes(replaced)).containsExactly(9);
    Path appended = dir.resolve("b.bin");
    FileSinkTransport append = new FileSinkTransport(appended, true);
    append.send(new byte[] {1, 2, 3});
    append.send(new byte[] {9});
    assertThat(Files.readAllBytes(appended)).containsExactly(1, 2, 3, 9);
  }

  @Test
  @DisplayName("works as a Transport lambda target and refuses null")
  void shouldRefuseNull_whenFileMissing() {
    assertThatNullPointerException().isThrownBy(() -> new FileSinkTransport(null, false));
    Transport t = data -> {};
    assertThat(t).isNotNull();
  }

  @Test
  @DisplayName("empty and oversize data are refused and write nothing")
  void shouldRefuse_whenDataEmptyOrTooBig() {
    Path file = dir.resolve("x.bin");
    FileSinkTransport sink = new FileSinkTransport(file, false);
    assertThatThrownBy(() -> sink.send(new byte[0])).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sink.send(new byte[Transport.MAX_BYTES + 1]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNullPointerException().isThrownBy(() -> sink.send(null));
    assertThat(file).doesNotExist();
  }

  @Test
  @DisplayName("exactly the maximum size is accepted")
  void shouldAccept_whenDataIsMaximum() throws IOException {
    Path file = dir.resolve("max.bin");
    new FileSinkTransport(file, false).send(new byte[Transport.MAX_BYTES]);
    assertThat(Files.size(file)).isEqualTo(Transport.MAX_BYTES);
  }
}
