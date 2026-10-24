/*
 * File:      TransportChecks.java
 * Module:    escpos-indic
 * Layer:     transport
 * Purpose:   Input checks shared by the transports.
 * Owns:      Data size validation.
 * Uses:      Transport.
 * Used by:   The transports in this package.
 * Not here:  Anything public.
 * Reuse:     Package-private.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import java.util.Objects;

/** Shared argument checks. */
final class TransportChecks {

  private TransportChecks() {}

  static void requireData(byte[] data) {
    Objects.requireNonNull(data, "data");
    if (data.length == 0 || data.length > Transport.MAX_BYTES) {
      throw new IllegalArgumentException(
          "data must be 1 to " + Transport.MAX_BYTES + " bytes but was " + data.length);
    }
  }
}
