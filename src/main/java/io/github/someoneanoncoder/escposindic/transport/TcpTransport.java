/*
 * File:      TcpTransport.java
 * Module:    escpos-indic
 * Layer:     transport
 * Purpose:   Sends the byte stream to a network printer's raw port (JetDirect style, TCP 9100).
 * Owns:      Connection with a timeout, a bounded write, graceful close, host and port validation.
 * Uses:      java.net.Socket, virtual threads for the write deadline.
 * Used by:   Ethernet and Wi-Fi receipt printers.
 * Not here:  Printer discovery, status polling, TLS.
 * Reuse:     Use only an address the shop administrator configured.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Opens a TCP connection per job, writes the stream and closes. Most network receipt printers
 * accept raw ESC/POS on port {@value #RAW_PORT}.
 *
 * <p>A TCP write can block forever when the printer stops reading (paper out, offline), so the
 * write has its own deadline: on timeout the socket is closed and an {@link IOException} is thrown.
 * The data is not encrypted or authenticated, as raw printing never is; keep the printer on the
 * shop's own network. The address must come from the shop's configuration, never from a customer or
 * a document.
 */
public final class TcpTransport implements Transport {

  /** The usual raw printing port. */
  public static final int RAW_PORT = 9100;

  private static final int MAX_HOST_LENGTH = 253;

  private final String host;
  private final int port;
  private final Duration connectTimeout;
  private final Duration sendTimeout;

  /**
   * Creates a transport.
   *
   * @param host host name or IP address, 1 to 253 characters without spaces or control characters
   * @param port 1 to 65535
   * @param connectTimeout longest wait to connect, more than zero
   * @param sendTimeout longest wait for the whole write, more than zero
   * @throws IllegalArgumentException if an argument is out of range
   */
  public TcpTransport(String host, int port, Duration connectTimeout, Duration sendTimeout) {
    Objects.requireNonNull(host, "host");
    if (host.isEmpty() || host.length() > MAX_HOST_LENGTH || hasBadCharacter(host)) {
      throw new IllegalArgumentException("host must be 1 to 253 characters without spaces");
    }
    if (port < 1 || port > 65_535) {
      throw new IllegalArgumentException("port must be 1 to 65535 but was " + port);
    }
    requirePositive(connectTimeout, "connectTimeout");
    requirePositive(sendTimeout, "sendTimeout");
    this.host = host;
    this.port = port;
    this.connectTimeout = connectTimeout;
    this.sendTimeout = sendTimeout;
  }

  /**
   * Creates a transport for the raw port with 3 seconds to connect and 10 seconds to write.
   *
   * @param host host name or IP address, see the constructor
   * @return the transport
   */
  public static TcpTransport rawPrinter(String host) {
    return new TcpTransport(host, RAW_PORT, Duration.ofSeconds(3), Duration.ofSeconds(10));
  }

  @Override
  public void send(byte[] data) throws IOException {
    TransportChecks.requireData(data);
    try (ExecutorService writer = Executors.newVirtualThreadPerTaskExecutor();
        Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), (int) connectTimeout.toMillis());
      Future<Void> write = writer.submit(() -> write(socket, data));
      await(write);
    }
  }

  private static Void write(Socket socket, byte[] data) throws IOException {
    socket.getOutputStream().write(data);
    socket.getOutputStream().flush();
    socket.shutdownOutput();
    return null;
  }

  private void await(Future<Void> write) throws IOException {
    try {
      write.get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      write.cancel(true);
      throw new IOException("the printer did not accept the data in time", e);
    } catch (InterruptedException e) {
      write.cancel(true);
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("interrupted while sending");
    } catch (ExecutionException e) {
      throw new IOException("could not send to the printer", e.getCause());
    }
  }

  private static boolean hasBadCharacter(String host) {
    for (int i = 0; i < host.length(); i++) {
      char c = host.charAt(i);
      if (c <= ' ' || c == 0x7F) {
        return true;
      }
    }
    return false;
  }

  private static void requirePositive(Duration value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be more than zero");
    }
  }
}
