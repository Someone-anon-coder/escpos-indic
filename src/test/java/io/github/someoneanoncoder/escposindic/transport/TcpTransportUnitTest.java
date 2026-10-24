/*
 * File:      TcpTransportUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks the TCP transport against a local server.
 * Owns:      Delivery of all bytes, large jobs, refused connections, write deadline.
 * Uses:      TcpTransport.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class TcpTransportUnitTest {

  private static CompletableFuture<byte[]> readAll(ServerSocket server) {
    return CompletableFuture.supplyAsync(
        () -> {
          try (Socket s = server.accept()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            s.getInputStream().transferTo(out);
            return out.toByteArray();
          } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
          }
        });
  }

  @Test
  @DisplayName("the server receives exactly the bytes sent")
  void shouldDeliverBytes_whenServerReads() throws Exception {
    try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      CompletableFuture<byte[]> received = readAll(server);
      new TcpTransport(
              "127.0.0.1", server.getLocalPort(), Duration.ofSeconds(2), Duration.ofSeconds(5))
          .send(new byte[] {0x1B, 0x40, 'H', 'i', 0x0A});
      assertThat(received.get()).containsExactly(0x1B, 0x40, 'H', 'i', 0x0A);
    }
  }

  @Test
  @DisplayName("a large image-sized job arrives complete")
  void shouldDeliverLargeJob_whenServerReads() throws Exception {
    byte[] data = new byte[1_000_000];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i * 31);
    }
    try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      CompletableFuture<byte[]> received = readAll(server);
      new TcpTransport(
              "127.0.0.1", server.getLocalPort(), Duration.ofSeconds(2), Duration.ofSeconds(10))
          .send(data);
      assertThat(received.get()).isEqualTo(data);
    }
  }

  @Test
  @DisplayName("each send is its own connection")
  void shouldReconnect_whenSentTwice() throws Exception {
    try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      TcpTransport t =
          new TcpTransport(
              "127.0.0.1", server.getLocalPort(), Duration.ofSeconds(2), Duration.ofSeconds(5));
      CompletableFuture<byte[]> first = readAll(server);
      t.send(new byte[] {1});
      assertThat(first.get()).containsExactly(1);
      CompletableFuture<byte[]> second = readAll(server);
      t.send(new byte[] {2});
      assertThat(second.get()).containsExactly(2);
    }
  }

  @Test
  @DisplayName("a closed port gives an IOException")
  void shouldFail_whenNothingListens() throws IOException {
    int port;
    try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      port = s.getLocalPort();
    }
    TcpTransport t =
        new TcpTransport("127.0.0.1", port, Duration.ofSeconds(2), Duration.ofSeconds(2));
    assertThatThrownBy(() -> t.send(new byte[] {1})).isInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("a printer that stops reading ends in a timeout, not a hang")
  void shouldTimeOut_whenPrinterNeverReads() throws Exception {
    Socket[] held = new Socket[1];
    try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      CompletableFuture<Void> accept =
          CompletableFuture.runAsync(
              () -> {
                try {
                  held[0] = server.accept();
                } catch (IOException e) {
                  throw new java.io.UncheckedIOException(e);
                }
              });
      TcpTransport t =
          new TcpTransport(
              "127.0.0.1", server.getLocalPort(), Duration.ofSeconds(2), Duration.ofMillis(400));
      long start = System.nanoTime();
      assertThatThrownBy(() -> t.send(new byte[Transport.MAX_BYTES]))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("in time");
      assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
      accept.get();
      held[0].close();
    }
  }

  @Test
  @DisplayName("rawPrinter uses port 9100")
  void shouldUseRawPort_whenCreatedForPrinter() {
    assertThat(TcpTransport.RAW_PORT).isEqualTo(9100);
    assertThat(TcpTransport.rawPrinter("192.168.1.50")).isNotNull();
  }
}
