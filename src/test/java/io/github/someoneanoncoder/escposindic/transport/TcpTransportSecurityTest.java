/*
 * File:      TcpTransportSecurityTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Proves bad addresses, ports, timeouts and data are refused before any connection.
 * Owns:      Host, port, timeout and size validation.
 * Uses:      TcpTransport.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.SecurityTest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SecurityTest
class TcpTransportSecurityTest {

  private static final Duration OK = Duration.ofSeconds(1);

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "a b", "host\n", "host\u0000", "ho\tst", "a\u007Fb"})
  @DisplayName("hosts with spaces or control characters are refused")
  void shouldRefuse_whenHostHasBadCharacters(String host) {
    assertThatThrownBy(() -> new TcpTransport(host, 9100, OK, OK))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("host length limit is 253")
  void shouldRefuse_whenHostTooLong() {
    assertThatThrownBy(() -> new TcpTransport("a".repeat(254), 9100, OK, OK))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> new TcpTransport("a".repeat(253), 9100, OK, OK));
    assertThatNullPointerException().isThrownBy(() -> new TcpTransport(null, 9100, OK, OK));
  }

  @Test
  @DisplayName("ports outside 1 to 65535 are refused")
  void shouldRefuse_whenPortOutOfRange() {
    assertThatThrownBy(() -> new TcpTransport("h", 0, OK, OK))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TcpTransport("h", 65_536, OK, OK))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TcpTransport("h", -1, OK, OK))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNoException().isThrownBy(() -> new TcpTransport("h", 1, OK, OK));
    assertThatNoException().isThrownBy(() -> new TcpTransport("h", 65_535, OK, OK));
  }

  @Test
  @DisplayName("timeouts must be positive")
  void shouldRefuse_whenTimeoutIsNotPositive() {
    assertThatThrownBy(() -> new TcpTransport("h", 9100, Duration.ZERO, OK))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TcpTransport("h", 9100, OK, Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNullPointerException().isThrownBy(() -> new TcpTransport("h", 9100, null, OK));
    assertThatNullPointerException().isThrownBy(() -> new TcpTransport("h", 9100, OK, null));
  }

  @Test
  @DisplayName("empty and oversize data are refused before connecting")
  void shouldRefuse_whenDataEmptyOrTooBig() {
    TcpTransport t = new TcpTransport("203.0.113.1", 9100, OK, OK);
    assertThatThrownBy(() -> t.send(new byte[0])).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> t.send(new byte[Transport.MAX_BYTES + 1]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNullPointerException().isThrownBy(() -> t.send(null));
  }
}
