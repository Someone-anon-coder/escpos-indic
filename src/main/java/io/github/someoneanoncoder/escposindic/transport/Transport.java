/*
 * File:      Transport.java
 * Module:    escpos-indic
 * Layer:     transport
 * Purpose:   Contract for sending a finished ESC/POS byte stream to somewhere.
 * Owns:      The single send operation and the size cap shared by all transports.
 * Uses:      Nothing.
 * Used by:   Callers, FileSinkTransport, TcpTransport, PrintQueueTransport.
 * Not here:  Building bytes (EscPosBuilder, ReceiptWriter).
 * Reuse:     Implement for other sinks, for example USB or Bluetooth serial.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import java.io.IOException;

/**
 * Delivers a complete ESC/POS byte stream to a printer or a test sink. Each call to {@link
 * #send(byte[])} is one print job: connect, write everything, finish.
 *
 * <p>Implementations are stateless between calls, so one instance can be reused and shared between
 * threads unless its Javadoc says otherwise.
 */
@FunctionalInterface
public interface Transport {

  /** Largest stream a transport accepts, 8 MiB, far above any receipt. */
  int MAX_BYTES = 8 * 1024 * 1024;

  /**
   * Sends one print job.
   *
   * @param data the complete byte stream, not null, 1 to {@value #MAX_BYTES} bytes
   * @throws IOException if the destination cannot be reached, refuses the data, or does not finish
   *     in time
   * @throws IllegalArgumentException if {@code data} is empty or larger than {@value #MAX_BYTES}
   */
  void send(byte[] data) throws IOException;
}
