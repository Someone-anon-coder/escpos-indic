/*
 * File:      PrintQueueTransportUnitTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Checks the print queue transport with a scripted fake queue.
 * Owns:      Raw document delivery, final events, failures, timeout, waiting for a late event.
 * Uses:      PrintQueueTransport, FakePrintQueue.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.someoneanoncoder.escposindic.testing.FakePrintQueue;
import io.github.someoneanoncoder.escposindic.testing.FakePrintQueue.Event;
import io.github.someoneanoncoder.escposindic.testing.UnitTest;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class PrintQueueTransportUnitTest {

  private static final Duration SECOND = Duration.ofSeconds(1);

  @ParameterizedTest
  @EnumSource(
      value = Event.class,
      names = {"COMPLETED", "TRANSFER_COMPLETE", "NO_MORE_EVENTS"})
  @DisplayName("the bytes reach the queue and a final success event ends the wait")
  void shouldDeliver_whenQueueReportsSuccess(Event event) throws IOException {
    FakePrintQueue queue = new FakePrintQueue().onJob(q -> q.fire(event));
    new PrintQueueTransport(queue.service(), SECOND).send(new byte[] {0x1B, 0x40, 'A'});
    assertThat(queue.jobs()).hasSize(1);
    assertThat(queue.jobs().get(0)).containsExactly(0x1B, 0x40, 'A');
  }

  @ParameterizedTest
  @EnumSource(
      value = Event.class,
      names = {"FAILED", "CANCELED"})
  @DisplayName("failed and cancelled jobs are errors")
  void shouldFail_whenQueueReportsFailure(Event event) {
    FakePrintQueue queue = new FakePrintQueue().onJob(q -> q.fire(event));
    assertThatThrownBy(() -> new PrintQueueTransport(queue.service(), SECOND).send(new byte[] {1}))
        .isInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("the first final event wins")
  void shouldKeepFirstOutcome_whenSeveralEventsArrive() throws IOException {
    FakePrintQueue queue =
        new FakePrintQueue()
            .onJob(
                q -> {
                  q.fire(Event.COMPLETED);
                  q.fire(Event.FAILED);
                });
    new PrintQueueTransport(queue.service(), SECOND).send(new byte[] {1});
  }

  @Test
  @DisplayName("a queue that refuses raw data is an error and gets no job")
  void shouldFail_whenRawNotSupported() {
    FakePrintQueue queue = new FakePrintQueue().rawSupported(false);
    assertThatThrownBy(() -> new PrintQueueTransport(queue.service(), SECOND).send(new byte[] {1}))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("raw");
    assertThat(queue.jobs()).isEmpty();
  }

  @Test
  @DisplayName("a PrintException from the platform becomes an IOException")
  void shouldFail_whenPrintThrows() {
    FakePrintQueue queue = new FakePrintQueue().failOnPrint(true);
    assertThatThrownBy(() -> new PrintQueueTransport(queue.service(), SECOND).send(new byte[] {1}))
        .isInstanceOf(IOException.class)
        .hasCauseInstanceOf(javax.print.PrintException.class);
  }

  @Test
  @DisplayName("no final event within the timeout is an error")
  void shouldTimeOut_whenNoFinalEvent() {
    FakePrintQueue queue = new FakePrintQueue().onJob(q -> {});
    assertThatThrownBy(
            () ->
                new PrintQueueTransport(queue.service(), Duration.ofMillis(200))
                    .send(new byte[] {1}))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("in time");
  }

  @Test
  @DisplayName("requires-attention does not end the wait, a later completion does")
  void shouldKeepWaiting_whenPaperOutThenCompleted() throws IOException {
    FakePrintQueue queue =
        new FakePrintQueue()
            .onJob(
                q -> {
                  q.fire(Event.REQUIRES_ATTENTION);
                  Thread.ofVirtual()
                      .start(
                          () -> {
                            try {
                              Thread.sleep(150);
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                            }
                            q.fire(Event.COMPLETED);
                          });
                });
    long start = System.nanoTime();
    new PrintQueueTransport(queue.service(), Duration.ofSeconds(3)).send(new byte[] {1});
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThan(Duration.ofMillis(100));
  }

  @Test
  @DisplayName("an unknown queue name is empty and queue names can be listed")
  void shouldBeEmpty_whenNameUnknown() {
    assertThat(PrintQueueTransport.named("no such printer ☃", SECOND)).isEmpty();
    assertThat(PrintQueueTransport.queueNames()).isNotNull();
  }

  @Test
  @DisplayName("null arguments and bad timeouts are refused")
  void shouldRefuse_whenArgumentsBad() {
    FakePrintQueue queue = new FakePrintQueue();
    assertThatNullPointerException().isThrownBy(() -> new PrintQueueTransport(null, SECOND));
    assertThatNullPointerException()
        .isThrownBy(() -> new PrintQueueTransport(queue.service(), null));
    assertThatThrownBy(() -> new PrintQueueTransport(queue.service(), Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PrintQueueTransport(queue.service(), Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatNullPointerException().isThrownBy(() -> PrintQueueTransport.named(null, SECOND));
    PrintQueueTransport t = new PrintQueueTransport(queue.service(), SECOND);
    assertThatThrownBy(() -> t.send(new byte[0])).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> t.send(new byte[Transport.MAX_BYTES + 1]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(queue.jobs()).isEmpty();
  }
}
